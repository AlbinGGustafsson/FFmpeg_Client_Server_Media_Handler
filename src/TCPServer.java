import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {

    public static void main(String[] args) throws IOException {
        int port = 12345;
        int updatePort = 12346;

        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port);
             ServerSocket updateServerSocket = new ServerSocket(updatePort)) {
            System.out.println("Server listening on port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Socket updateSocket = updateServerSocket.accept(); // Create separate update socket for each client
                    UpdateServerThread updateServerThread = new UpdateServerThread(updateSocket);
                    executor.submit(updateServerThread); // Start thread for updates
                    executor.submit(() -> handleClient(clientSocket, updateServerThread)); // Start thread for main client handling
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void handleClient(Socket clientSocket, UpdateServerThread updateServerThread) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            System.out.println("Client connected");

            FileWrapper receivedFileWrapper = (FileWrapper) in.readObject();
            String receivedFileName = "fromclient_" + receivedFileWrapper.getFileName();
            Files.write(Paths.get(receivedFileName), receivedFileWrapper.getFileBytes());
            System.out.println("File received and saved.");

            updateServerThread.sendMessage("File process started!");
            executeFFmpegCommand(receivedFileName, "processed_" + receivedFileWrapper.getFileName(), updateServerThread, receivedFileWrapper.getCommand());
            updateServerThread.sendMessage("File process done!");

            File processedFile = new File("processed_" + receivedFileWrapper.getFileName());
            byte[] processedFileBytes = Files.readAllBytes(processedFile.toPath());
            FileWrapper processedFileWrapper = new FileWrapper(processedFile.getName(), new String[]{"done command"}, processedFileBytes);
            out.writeObject(processedFileWrapper);
            System.out.println("Processed file sent back to client.");
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    //Todo lär ju itne ta emot String inputFileName, String outputFileName, borde räcka med commandot? Eller bygga kommandot med output och cmd som innehåller input?
    private static void executeFFmpegCommand(String inputFileName, String outputFileName, UpdateServerThread updateThread, String[] cmd) {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        //TODO fixa här, ska ju inte skriva över
        cmd = new String[]{
                "ffmpeg",
                "-i",
                inputFileName,
                "-c:v",
                "libx264",
                "-b:v",
                "2000k",
                "-c:a",
                "aac",
                "-b:a",
                "128k",
                outputFileName
        };

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();

            // Thread to read from stdout
            new Thread(() -> {
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String s;
                try {
                    while ((s = stdInput.readLine()) != null) {
                        //System.out.println(s);
                        updateThread.sendMessage(s);

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
                        //System.err.println(s);
                        updateThread.sendMessage(s);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            int exitCode = process.waitFor();
            System.out.println("Exit Code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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

        public void sendMessage(String message) {
            if (updateOut != null) {
                updateOut.println(message);
            } else {
                System.err.println("Client not connected, cannot send message");
            }
        }
    }

}