import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPClient extends JFrame {
    private JTextArea logArea;

    public TCPClient(String serverAddress, int serverPort, int updatePort, String filePath, String ffmpegCommand) throws IOException {
        // GUI setup for log window
        setTitle("Log Window");
        logArea = new JTextArea(10, 70);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane);
        pack();
        setVisible(true);

        UpdateListener updateListener = new UpdateListener(serverAddress, updatePort, logArea);
        FileTransferTask fileTransfer = new FileTransferTask(serverAddress, serverPort, filePath, logArea, ffmpegCommand);

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(updateListener);
        executor.submit(fileTransfer);
    }
    static class FileTransferTask implements Runnable {
        private String serverAddress;
        private int serverPort;
        private String filePath;
        private JTextArea logAreaReference;

        private String ffmpegCommand;

        public FileTransferTask(String serverAddress, int serverPort, String filePath, JTextArea logArea, String ffmpegCommand) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.filePath = filePath;
            this.logAreaReference = logArea;
            this.ffmpegCommand = ffmpegCommand;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                File fileToSend = new File(filePath);
                byte[] fileBytes = Files.readAllBytes(fileToSend.toPath());
                FileWrapper fileWrapper = new FileWrapper(fileToSend.getName(), ffmpegCommand, fileBytes);
                out.writeObject(fileWrapper);
                log("FileWrapper sent successfully.");

                FileWrapper receivedWrapper = (FileWrapper) in.readObject();
                String receivedFileName = "fromserver_" + receivedWrapper.getFileName();
                Files.write(Paths.get(receivedFileName), receivedWrapper.getFileBytes());
                log("Processed file received successfully.");

            } catch (IOException | ClassNotFoundException e) {
                log(e.getMessage());
            }
        }

        private void log(String message) {
            if (logAreaReference != null) {
                SwingUtilities.invokeLater(() -> {
                    logAreaReference.append(message + "\n");
                });
            }
        }
    }

    static class UpdateListener implements Runnable {
        private String serverAddress;
        private int updatePort;
        private JTextArea logAreaReference;  // A reference to the logArea from TCPClient

        public UpdateListener(String serverAddress, int updatePort, JTextArea logArea) {
            this.serverAddress = serverAddress;
            this.updatePort = updatePort;
            this.logAreaReference = logArea;  // Storing the reference
        }

        @Override
        public void run() {
            try (Socket updateSocket = new Socket(serverAddress, updatePort);
                 BufferedReader updateIn = new BufferedReader(new InputStreamReader(updateSocket.getInputStream()))) {
                log("Update connection established");
                String updateMsg;
                while ((updateMsg = updateIn.readLine()) != null) {
                    log("Received update from server: " + updateMsg);
                }
            } catch (IOException e) {
                log(e.getMessage());
            }
        }

        private void log(String message) {
            // Using the logArea reference to append messages
            if (logAreaReference != null) {
                SwingUtilities.invokeLater(() -> {
                    logAreaReference.append(message + "\n");
                });
            }
        }
    }
}