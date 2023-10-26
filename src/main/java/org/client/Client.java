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

/**
 * Klientklass för att skicka filer och lyssna på uppdateringar från servern.
 * Det är också ett swing fönster för ett "jobb" och innehåller en log-ruta.
 */
public class Client extends JFrame {
    private JTextArea logArea;
    private FileTransferTask fileTransfer;
    private UpdateListener updateListener;

    /**
     * Konstruktor för Client-klassen.
     * Skapar en FileTransferTask för att påbörja uppladdning/hantering/nedladdning.
     *
     * @param serverAddress    Serveradressen dit filen ska skickas.
     * @param serverPort       Porten som används för att kommunicera med servern.
     * @param updatePort       Porten för att lyssna på uppdateringar från servern.
     * @param filePath         Sökvägen till den fil som ska skickas.
     * @param ffmpegCommand    FFmpeg-kommandot som ska användas för filbehandling.
     * @param outputFileName   Namnet på den resulterande filen efter behandling.
     * @param password         Lösenordet som används för autentisering.
     * @throws IOException     Kastas om det uppstår ett IO-fel.
     */
    public Client(String serverAddress, int serverPort, int updatePort, String filePath, String ffmpegCommand, String outputFileName, String password) throws IOException {
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

    /**
     * En inre klass som hanterar filöverföringen till servern.
     */
    static class FileTransferTask implements Runnable {
        private String serverAddress;
        private int serverPort;
        private String filePath;
        private String outputFileName;
        private JTextArea logAreaReference;
        private String ffmpegCommand;

        private String password;
        private Socket socket;

        /**
         * Konstruktor för FileTransferTask-klassen.
         *
         * @param serverAddress       Serveradressen dit filen ska skickas.
         * @param serverPort          Porten som används för att kommunicera med servern.
         * @param filePath            Sökvägen till den fil som ska skickas.
         * @param logArea             JTextArea för att logga händelser.
         * @param ffmpegCommand       FFmpeg-kommandot som ska användas för filbehandling.
         * @param outputFileName      Namnet på den resulterande filen efter behandling.
         * @param password            Lösenordet som används för autentisering.
         */
        public FileTransferTask(String serverAddress, int serverPort, String filePath, JTextArea logArea, String ffmpegCommand, String outputFileName, String password) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.filePath = filePath;
            this.logAreaReference = logArea;
            this.ffmpegCommand = ffmpegCommand;
            this.outputFileName = outputFileName;
            this.password = password;
        }

        /**
         * Metod som utför filöverföringen till servern.
         */
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

        /**
         * Metod för att stänga anslutningen till servern.
         */
        public void close() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Metod som appendar till textarean som visas i gui:n.
         */
        private void log(String message) {
            if (logAreaReference != null) {
                SwingUtilities.invokeLater(() -> {
                    logAreaReference.append(message + "\n");
                });
            }
        }
    }

    /**
     * En inre klass som lyssnar på uppdateringar från servern.
     */
    static class UpdateListener implements Runnable {
        private String serverAddress;
        private int updatePort;
        private JTextArea logAreaReference;

        private Socket updateSocket;

        /**
         * Konstruktor för UpdateListener-klassen.
         *
         * @param serverAddress       Serveradressen där uppdateringar ska lyssnas på.
         * @param updatePort          Porten för att lyssna på uppdateringar från servern.
         * @param logArea             JTextArea för att logga händelser.
         */
        public UpdateListener(String serverAddress, int updatePort, JTextArea logArea) {
            this.serverAddress = serverAddress;
            this.updatePort = updatePort;
            this.logAreaReference = logArea;
        }

        /**
         * Metod som lyssnar på uppdateringar från servern.
         */
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

        /**
         * Metod för att stänga anslutningen för "uppdateringslyssning".
         */
        public void close() {
            if (updateSocket != null && !updateSocket.isClosed()) {
                try {
                    updateSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Metod som appendar till textarean som visas i gui:n.
         */
        private void log(String message) {
            if (logAreaReference != null) {
                SwingUtilities.invokeLater(() -> {
                    logAreaReference.append(message + "\n");
                });
            }
        }
    }
}