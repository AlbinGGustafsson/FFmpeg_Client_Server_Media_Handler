package org.server;

import org.shared.ConnectionInfo;
import org.shared.SQliteManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Klass som representerar den grafiska användargränssnittet för servern.
 */

public class ServerGUI extends JFrame {

    private static SQliteManager manager;
    private JTextField filePortField;
    private JTextField updatePortField;
    private JButton startButton;
    private DefaultListModel<ConnectionInfo> listModel;
    private JList<ConnectionInfo> connectionList;
    private JTextArea logArea;

    private ArrayList<Process> processes;

    /**
     * Konstruktor för ServerGUI-klassen.
     * Sätter upp gränssnittet.
     */
    public ServerGUI() {

        processes = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanUp();
        }));


        // Set up the frame
        setTitle("FFmpeg Server");
        setSize(800, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);  // Center the frame

        // Set up the components
        filePortField = new JTextField(5);
        updatePortField = new JTextField(5);
        startButton = new JButton("Start Server");
        listModel = new DefaultListModel<>();
        connectionList = new JList<>(listModel);
        logArea = new JTextArea();
        logArea.setEditable(false);  // Make the log area uneditable
        JButton cleanCacheButton = new JButton("Clean Cache");
        JButton addPasswordButton = new JButton("Add Password");
        JButton databaseButton = new JButton("Database");


        databaseButton.addActionListener(e -> {
            new DatabaseWindow(manager).show();
        });

        addPasswordButton.addActionListener(e -> {

            AddPasswordDialog dialog = new AddPasswordDialog(this); // Replace 'yourMainFrameInstance' with the instance of your JFrame
            dialog.display();
            if (dialog.isConfirmed()) {
                String ip = dialog.getIpAddress();
                String password = dialog.getPassword();
                logMessage("Password added!");
                try {
                    manager.addClientPassword(ip, password);
                } catch (Exception ex) {
                    logMessage("Could not add password");
                }
            }

        });


        cleanCacheButton.addActionListener(e -> cleanCacheFiles());

        // Set up the layout
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.add(databaseButton);
        topPanel.add(addPasswordButton);
        topPanel.add(cleanCacheButton);
        topPanel.add(new JLabel("File Port:"));
        topPanel.add(filePortField);
        topPanel.add(new JLabel("Update Port:"));
        topPanel.add(updatePortField);
        topPanel.add(startButton);
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(connectionList), BorderLayout.WEST);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Set up the button action
        startButton.addActionListener(new StartServerAction());

        setVisible(true);
    }

    /**
     * Metod för att lägga till en process i en samling med processer.
     * Ifall servern stängs ner så finns alla processer den skapat i denna samling.
     * Då kan det städas upp.
     *
     * @param process Den aktiva processen att lägga till.
     */
    public void addProcess(Process process){
        processes.add(process);
    }

    /**
     * Metod för att ta bort en process från processsamlingen.
     *
     * @param process Den aktiva processen att ta bort.
     */
    public void removeProcess(Process process){
        processes.remove(process);
    }

    /**
     * Metod för att rensa upp och avsluta aktiva processer.
     */
    private void cleanUp() {
        System.out.println(processes.size() + " active processes");
        for (Process process : processes) {
            if (process.isAlive()) {
                process.destroy();
            }
        }
    }

    /**
     * Metod för att rensa cachelagrade filer.
     */
    private void cleanCacheFiles(){
        File cacheDir = new File("cachedFiles");
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            for (File file : cacheDir.listFiles()) {
                if (!file.delete()) {
                    System.err.println("Failed to delete file: " + file.getAbsolutePath());
                }
            }
            if (!cacheDir.delete()) {
                System.err.println("Failed to delete directory: " + cacheDir.getAbsolutePath());
            }
        }
    }

    /**
     * Inre klass som representerar en "action" för att starta servern.
     * Egen klass på grund av dess komplexitet.
     */
    private class StartServerAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String ffmpegVersion = getFFmpegVersion();
            if (ffmpegVersion == null) {
                logMessage("ffmpeg is not installed on this computer. Please install it before starting the server.");
                return;
            } else {
                logMessage("FFmpeg version installed: " + ffmpegVersion);
            }

            int filePort = Integer.parseInt(filePortField.getText());
            int updatePort = Integer.parseInt(updatePortField.getText());

            new Thread(() -> {
                try {
                    logMessage("Server started on file port " + filePort + " and update port " + updatePort);
                    Server server = new Server(ServerGUI.this, manager);
                    server.start(filePort, updatePort);
                } catch (IOException ex) {
                    logMessage("Failed to start server: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }).start();
        }

        /**
         * Metod för att hämta en FFmpeg-version om den kan hittas på datorn.
         *
         * @return FFmpeg-versionen eller null om FFmpeg inte är installerat.
         */
        private String getFFmpegVersion() {
            try {
                Process process = Runtime.getRuntime().exec("ffmpeg -version");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String firstLine = reader.readLine();
                process.waitFor();

                if (process.exitValue() == 0 && firstLine != null) {
                    return firstLine.split(" ")[2];
                }
            } catch (Exception e) {
                //Här skulle jag kunna hantera en exception
            }
            return null;
        }
    }

    /**
     * Metod för att lägga till en anslutning i listan över anslutningar (visas i gui:t).
     *
     * @param connectionInfo Informationen om anslutningen som ska läggas till.
     */
    public void addConnection(ConnectionInfo connectionInfo) {
        listModel.addElement(connectionInfo);
        logMessage(connectionInfo + " Started!");
    }

    /**
     * Metod för att ta bort en anslutning från listan över anslutningar. (tas bort från gui:t)
     *
     * @param connectionInfo Informationen om anslutningen som ska tas bort.
     * @param message        Meddelandet som ska visas i loggrutan.
     */
    public void removeConnection(ConnectionInfo connectionInfo, String message) {
        if (listModel.removeElement(connectionInfo)){
            logMessage(connectionInfo + message);
        }
    }

    /**
     * Metod för att logga ett meddelande i loggrutan på gränssnittet.
     *
     * @param message Meddelandet att logga.
     */
    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    /**
     * Metod för att starta serverapplikationen.
     * Argumentet som programmet tar emot är en encryption key för databasens krypterade lösenord.
     *
     * @param args Argumenten som tillhandahålls vid körning av programmet.
     * @throws Exception Om det uppstår ett undantag.
     */
    public static void main(String[] args) throws Exception {

        if(args.length < 1) {
            System.out.println("Please provide the encryption key as an argument.");
            System.exit(1);
        }
        String encryptionKey = args[0];
        manager = new SQliteManager(encryptionKey);

        new ServerGUI();
    }
}