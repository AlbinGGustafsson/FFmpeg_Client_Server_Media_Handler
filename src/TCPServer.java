import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {

    private static final Random RANDOM = new Random();

    public static void startServer(int filePort, int updatePort, ServerGUI gui) throws IOException {

        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(filePort);
             ServerSocket updateServerSocket = new ServerSocket(updatePort)) {

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Socket updateSocket = updateServerSocket.accept(); // Create separate update socket for each client
                    UpdateServerThread updateServerThread = new UpdateServerThread(updateSocket);
                    executor.submit(updateServerThread); // Start thread for updates
                    executor.submit(() -> handleClient(clientSocket, updateServerThread, gui)); // Start thread for main client handling
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

//    public static void main(String[] args) throws IOException {
//        int port = 12345;
//        int updatePort = 12346;
//
//        ExecutorService executor = Executors.newCachedThreadPool();
//
//        try (ServerSocket serverSocket = new ServerSocket(port);
//             ServerSocket updateServerSocket = new ServerSocket(updatePort)) {
//            System.out.println("Server listening on port " + port);
//
//            while (true) {
//                try {
//                    Socket clientSocket = serverSocket.accept();
//                    Socket updateSocket = updateServerSocket.accept(); // Create separate update socket for each client
//                    UpdateServerThread updateServerThread = new UpdateServerThread(updateSocket);
//                    executor.submit(updateServerThread); // Start thread for updates
//                    executor.submit(() -> handleClient(clientSocket, updateServerThread)); // Start thread for main client handling
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    private static String generateUniqueIdentifier() {
        String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyz";
        final int IDENTIFIER_LENGTH = 4;

        StringBuilder identifier = new StringBuilder(IDENTIFIER_LENGTH);
        for (int i = 0; i < IDENTIFIER_LENGTH; i++) {
            identifier.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return identifier.toString();
    }

    private static void handleClient(Socket clientSocket, UpdateServerThread updateServerThread, ServerGUI gui) {
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

            connectionInfo = new ConnectionInfo(clientSocket.getInetAddress().toString(), uniqueFileName);
            gui.addConnection(connectionInfo);

            //String receivedFileName = "fromclient_" + receivedFileWrapper.getFileName();
            receivedPath = receivedFileName;
            Files.write(Paths.get(receivedPath), receivedFileWrapper.getFileBytes());
            System.out.println("File received and saved.");

            updateServerThread.sendMessage("File process started!");
            outputFileName = "processed_" + identifier + "_" + receivedFileWrapper.getOutputFileName();
            ffmpegProcess = executeFFmpegCommand(receivedFileName, outputFileName, updateServerThread, receivedFileWrapper.getCommand());
            updateServerThread.sendMessage("File process done!");

            File processedFile = new File(outputFileName);
            byte[] processedFileBytes = Files.readAllBytes(processedFile.toPath());
            FileWrapper processedFileWrapper = new FileWrapper(processedFile.getName(), "done command", processedFileBytes, "none");
            out.writeObject(processedFileWrapper);
            System.out.println("Processed file sent back to client.");

            gui.removeConnection(connectionInfo, "Finished");

        } catch (ClassNotFoundException | IOException e) {

            System.err.println("Something went wrong: " + e.getMessage());
            gui.removeConnection(connectionInfo, " Something went wrong, trying to terminate ffmpeg and delete files");

            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                ffmpegProcess.destroy();  // Terminate the FFmpeg process
            }

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

    //Todo lär ju itne ta emot String inputFileName, String outputFileName, borde räcka med commandot? Eller bygga kommandot med output och cmd som innehåller input?
    private static Process executeFFmpegCommand(
            String inputFileName,
            String outputFileName,
            UpdateServerThread updateThread,
            String ffmpegCommand
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

            // Thread to read from stdout
            new Thread(() -> {
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String s;
                try {
                    while ((s = stdInput.readLine()) != null) {
                        if (!updateThread.sendMessage(s)){
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
                        if (!updateThread.sendMessage(s)){
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
            return process;  // Returning the process object here

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;  // Return null or handle the exception in a way that's appropriate for your application.
        }
    }

    static class UpdateServerThread implements Runnable {
        private Socket updateSocket;
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