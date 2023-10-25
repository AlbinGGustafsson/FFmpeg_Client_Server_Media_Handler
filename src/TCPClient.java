import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPClient {
    public static void main(String[] args) throws IOException {
        String serverAddress = "127.0.0.1"; // Replace with the server's IP address or hostname
        int serverPort = 12345;
        int updatePort = 12346;
        String filePath = args[0]; // Replace with the path to the file you want to send

        UpdateListener updateListener = new UpdateListener(serverAddress, updatePort);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(updateListener);

        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {


            File fileToSend = new File(filePath);
            byte[] fileBytes = Files.readAllBytes(fileToSend.toPath());
            FileWrapper fileWrapper = new FileWrapper(fileToSend.getName(), new String[]{"command"}, fileBytes);
            out.writeObject(fileWrapper);
            System.out.println("FileWrapper sent successfully.");

            FileWrapper receivedWrapper = (FileWrapper) in.readObject();
            String receivedFileName = "fromserver_" + receivedWrapper.getFileName();
            Files.write(Paths.get(receivedFileName), receivedWrapper.getFileBytes());
            System.out.println("Processed file received successfully.");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
    }


    static class UpdateListener implements Runnable {
        private String serverAddress;
        private int updatePort;

        public UpdateListener(String serverAddress, int updatePort) {
            this.serverAddress = serverAddress;
            this.updatePort = updatePort;
        }

        @Override
        public void run() {
            try (Socket updateSocket = new Socket(serverAddress, updatePort);
                 BufferedReader updateIn = new BufferedReader(new InputStreamReader(updateSocket.getInputStream()))) {
                System.out.println("Update connection established");
                String updateMsg;
                while ((updateMsg = updateIn.readLine()) != null) {
                    System.out.println("Received update from server: " + updateMsg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
