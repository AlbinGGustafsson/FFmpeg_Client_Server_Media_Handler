package org.client;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Klass som hanterar grafiskt användargränssnitt (GUI) för klienten.
 */
public class ClientGUI {

    private JFrame frame;
    private JTextField serverAddressField;

    private JTextField passwordField;
    private JTextField serverPortField;
    private JTextField updatePortField;
    private JTextField ffmpegCommandField;

    private JTextField customOutputFileName;

    private String filePath;

    /**
     * Konstruktor för ClientGUI-klassen som skapar och konfigurerar det grafiska användargränssnittet.
     */
    public ClientGUI() {

        frame = new JFrame("FFmpeg Client");
        frame.setSize(800, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(7, 2));

        panel.add(new JLabel("Server Address:"));
        serverAddressField = new JTextField("127.0.0.1");
        panel.add(serverAddressField);

        panel.add(new JLabel("Password:"));
        passwordField = new JTextField("password");
        panel.add(passwordField);

        panel.add(new JLabel("Server Port:"));
        serverPortField = new JTextField("12345");
        panel.add(serverPortField);

        panel.add(new JLabel("Update Port:"));
        updatePortField = new JTextField("12346");
        panel.add(updatePortField);

        panel.add(new JLabel("FFmpeg Command: (%s for input and output)"));

        //Todo Här ser man att %s är en del av commandot för input och output..... Går nog att hitta en snyggare lösning
        ffmpegCommandField = new JTextField("ffmpeg -i %s -c:v libx264 -b:v 2000k -c:a aac -b:a 128k %s");
        panel.add(ffmpegCommandField);

        panel.add(new JLabel("Output filename (will include identifier)"));
        customOutputFileName = new JTextField("");
        panel.add(customOutputFileName);


        JLabel filePathLabel = new JLabel("File name");
        panel.add(filePathLabel);

        JButton filePickerButton = new JButton("Pick File");
        JButton startButton = new JButton("Start");
        startButton.setEnabled(false); // Initially disable the button
        startButton.addActionListener(e -> startTCPClient());

        filePickerButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                filePath = fileChooser.getSelectedFile().getAbsolutePath();
                filePathLabel.setText(fileChooser.getSelectedFile().getName());
                customOutputFileName.setText(fileChooser.getSelectedFile().getName());
                startButton.setEnabled(true);
            }
        });

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new FlowLayout());
        southPanel.add(filePickerButton);
        southPanel.add(startButton);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(southPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }


    /**
     * Metod för att starta en klient och initiera kommunikationen med servern.
     */
    private void startTCPClient() {
        String serverAddress = serverAddressField.getText();
        int serverPort = Integer.parseInt(serverPortField.getText());
        int updatePort = Integer.parseInt(updatePortField.getText());
        String ffmpegCommand = ffmpegCommandField.getText();
        String outputFileName = customOutputFileName.getText();
        String password = passwordField.getText();

        try {
            new Client(serverAddress, serverPort, updatePort, filePath, ffmpegCommand, outputFileName, password);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error starting TCP Client: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI());
    }
}