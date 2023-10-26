package org.server;

import org.shared.ConnectionInfo;
import org.shared.FileWrapper;
import org.shared.JobStatus;
import org.shared.SQliteManager;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {

    private static final String CACHE_DIR = "cachedFiles";

    private final ExecutorService executor;
    private final ServerGUI gui;
    private final Random random = new Random();

    private SQliteManager dbManager;

    public TCPServer(ServerGUI gui, SQliteManager dbManager) {
        this.gui = gui;
        this.dbManager = dbManager;
        this.executor = Executors.newCachedThreadPool();
    }

    public void start(int filePort, int updatePort) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(filePort);
             ServerSocket updateServerSocket = new ServerSocket(updatePort)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Socket updateSocket = updateServerSocket.accept();

                UpdateServerThread updateThread = new UpdateServerThread(updateSocket);
                executor.submit(updateThread);

                ClientHandler clientHandler = new ClientHandler(clientSocket, updateThread, gui);
                executor.submit(clientHandler);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final UpdateServerThread updateServerThread;
        private final ServerGUI gui;

        public ClientHandler(Socket clientSocket, UpdateServerThread updateServerThread, ServerGUI gui) {
            this.clientSocket = clientSocket;
            this.updateServerThread = updateServerThread;
            this.gui = gui;
        }

        @Override
        public void run() {
            handleClient(clientSocket, updateServerThread, gui);
        }

        private void handleClient(Socket clientSocket, UpdateServerThread updateServerThread, ServerGUI gui) {
            String receivedPath = "";
            String outputFileName = "";
            ConnectionInfo connectionInfo = null;
            Process ffmpegProcess = null;

            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

                System.out.println("Client connected");

                FileWrapper receivedFileWrapper = (FileWrapper) in.readObject();

                String identifier = generateUniqueIdentifier();

                String uniqueFileName = identifier + "_" + receivedFileWrapper.getFileName();
                String receivedFileName = "fromclient_" + uniqueFileName;

                String ipAdress = clientSocket.getInetAddress().getHostAddress();

                connectionInfo = new ConnectionInfo(ipAdress, uniqueFileName);
                gui.addConnection(connectionInfo);

                //Lägger till jobbet i databasen
                dbManager.addJob(uniqueFileName, ipAdress, LocalDateTime.now().toString(), JobStatus.STARTED);

                // Ensure the directory exists before writing
                ensureDirectoryExists();

                receivedPath = CACHE_DIR + "/" + receivedFileName;
                Files.write(Paths.get(receivedPath), receivedFileWrapper.getFileBytes());
                System.out.println("File received and saved.");

                updateServerThread.sendMessage("File process started!");
                outputFileName = CACHE_DIR + "/" + "processed_" + identifier + "_" + receivedFileWrapper.getOutputFileName();
                ffmpegProcess = executeFFmpegCommand(receivedPath, outputFileName, updateServerThread, receivedFileWrapper.getCommand(), gui);
                updateServerThread.sendMessage("File process done!");

                File processedFile = new File(outputFileName);
                byte[] processedFileBytes = Files.readAllBytes(processedFile.toPath());
                FileWrapper processedFileWrapper = new FileWrapper(processedFile.getName(), "done command", processedFileBytes, "none");
                out.writeObject(processedFileWrapper);
                System.out.println("Processed file sent back to client.");

                gui.removeConnection(connectionInfo, "Finished");
                dbManager.addJob(uniqueFileName, ipAdress, LocalDateTime.now().toString(), JobStatus.FINISHED);

            } catch (ClassNotFoundException | IOException e) {

                System.err.println("Something went wrong: " + e.getMessage());
                gui.removeConnection(connectionInfo, " Something went wrong, trying to terminate ffmpeg and delete files");

                if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                    ffmpegProcess.destroy();  // Terminate the FFmpeg process
                }

            } catch (SQLException e) {
                System.err.println("Could not write to database");
            } finally {
                // Delete the received and processed files

                try {
                    Files.deleteIfExists(Paths.get(receivedPath));
                    Files.deleteIfExists(Paths.get(outputFileName));
                } catch (IOException e) {
                    System.err.println("Could not remove files");
                }
                System.out.println("Files deleted.");

            }
        }

        private String generateUniqueIdentifier() {
            String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyz";
            final int IDENTIFIER_LENGTH = 4;
            StringBuilder identifier = new StringBuilder(IDENTIFIER_LENGTH);
            for (int i = 0; i < IDENTIFIER_LENGTH; i++) {
                identifier.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            return identifier.toString();
        }

        private void ensureDirectoryExists() {
            Path path = Paths.get(CACHE_DIR);
            if (Files.notExists(path)) {
                try {
                    Files.createDirectory(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        private Process executeFFmpegCommand(
                String inputFileName,
                String outputFileName,
                UpdateServerThread updateThread,
                String ffmpegCommand,
                ServerGUI gui
        ) {

            ffmpegCommand = String.format(
                    ffmpegCommand,
                    inputFileName,
                    outputFileName
            );

            String[] cmdArray = ffmpegCommand.split(" ");

            ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
            try {
                Process process = processBuilder.start();
                gui.addProcess(process);

                // Thread to read from stdout
                new Thread(() -> {
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String s;
                    try {
                        while ((s = stdInput.readLine()) != null) {
                            if (!updateThread.sendMessage(s)) {
                                if (process.isAlive()) {
                                    process.destroy();  // Terminate the FFmpeg process
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                // Thread to read from stderr
                new Thread(() -> {
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String s;
                    try {
                        while ((s = stdError.readLine()) != null) {
                            if (!updateThread.sendMessage(s)) {
                                if (process.isAlive()) {
                                    process.destroy();  // Terminate the FFmpeg process
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                process.waitFor();  // Note: This line will block until the process completes.
                gui.removeProcess(process);
                return process;  // Returning the process object here

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return null;  // Return null or handle the exception in a way that's appropriate for your application.
            }
        }
    }

    public static class UpdateServerThread implements Runnable {
        private final Socket updateSocket;
        private PrintWriter updateOut;

        public UpdateServerThread(Socket updateSocket) {
            this.updateSocket = updateSocket;
        }

        @Override
        public void run() {
            try {
                updateOut = new PrintWriter(updateSocket.getOutputStream(), true);
                System.out.println("Update client connected");
                while (true) {
                    // Waiting for sendMessage to be called
                    Thread.sleep(1000);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean sendMessage(String message) {
            if (updateOut == null) {
                System.err.println("Client not connected, cannot send message");
                return false;
            }

            updateOut.println(message);

            if (updateOut.checkError()) {
                System.err.println("Error sending message to client");
                return false;
            }

            return true;
        }
    }

}