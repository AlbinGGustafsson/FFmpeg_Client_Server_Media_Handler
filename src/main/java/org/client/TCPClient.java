package org.client;

import org.shared.FileWrapper;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TCPClient extends JFrame {
    private JTextArea logArea;

    private FileTransferTask fileTransfer;
    private UpdateListener updateListener;

    public TCPClient(String serverAddress, int serverPort, int updatePort, String filePath, String ffmpegCommand, String outputFileName, String password) throws IOException {
        setTitle("Log Window");
        logArea = new JTextArea(20, 50);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane);
        pack();
        setVisible(true);

        updateListener = new UpdateListener(serverAddress, updatePort, logArea);
        fileTransfer = new FileTransferTask(serverAddress, serverPort, filePath, logArea, ffmpegCommand, outputFileName, password);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                updateListener.close();
                fileTransfer.close();
            }
        });

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(updateListener);
        executor.submit(fileTransfer);
    }

    static class FileTransferTask implements Runnable {
        private String serverAddress;
        private int serverPort;
        private String filePath;
        private String outputFileName;
        private JTextArea logAreaReference;
        private String ffmpegCommand;

        private String password;
        private Socket socket;

        public FileTransferTask(String serverAddress, int serverPort, String filePath, JTextArea logArea, String ffmpegCommand, String outputFileName, String password) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.filePath = filePath;
            this.logAreaReference = logArea;
            this.ffmpegCommand = ffmpegCommand;
            this.outputFileName = outputFileName;
            this.password = password;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(serverAddress, serverPort);

                try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    out.writeObject(password);

                    File fileToSend = new File(filePath);
                    byte[] fileBytes = Files.readAllBytes(fileToSend.toPath());
                    FileWrapper fileWrapper = new FileWrapper(fileToSend.getName(), ffmpegCommand, fileBytes, outputFileName);
                    out.writeObject(fileWrapper);
                    log("File sent successfully.");

                    FileWrapper receivedWrapper = (FileWrapper) in.readObject();
                    String receivedFileName = "fromserver_" + receivedWrapper.getFileName();
                    Files.write(Paths.get(receivedFileName), receivedWrapper.getFileBytes());
                    log("Processed file received successfully.");

                } catch (IOException | ClassNotFoundException e) {
                    log(e.getMessage());
                }
            } catch (IOException e) {
                log(e.getMessage());
            }
        }

        public void close() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        private JTextArea logAreaReference;

        private Socket updateSocket;

        public UpdateListener(String serverAddress, int updatePort, JTextArea logArea) {
            this.serverAddress = serverAddress;
            this.updatePort = updatePort;
            this.logAreaReference = logArea;
        }

        @Override
        public void run() {
            try {
                updateSocket = new Socket(serverAddress, updatePort);

                try (BufferedReader updateIn = new BufferedReader(new InputStreamReader(updateSocket.getInputStream()))) {
                    log("Update connection established");
                    String updateMsg;
                    while ((updateMsg = updateIn.readLine()) != null) {
                        log("Received update from server: " + updateMsg);
                    }
                } catch (IOException e) {
                    log(e.getMessage());
                }
            } catch (IOException e) {
                log(e.getMessage());
            }
        }

        public void close() {
            if (updateSocket != null && !updateSocket.isClosed()) {
                try {
                    updateSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
}