import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class TCPClientGUI {

    private JFrame frame;
    private JTextField serverAddressField;
    private JTextField serverPortField;
    private JTextField updatePortField;
    private JTextField ffmpegCommandField;

    private JTextField customOutputFileName;
    private JTextArea logArea;

    private String filePath;

    public TCPClientGUI() {
        frame = new JFrame("TCP Client");
        frame.setSize(800, 300); // Increased size a bit for better layout
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 2));

        // Server Address
        panel.add(new JLabel("Server Address:"));
        serverAddressField = new JTextField("127.0.0.1");
        panel.add(serverAddressField);

        // Server Port
        panel.add(new JLabel("Server Port:"));
        serverPortField = new JTextField("12345");
        panel.add(serverPortField);

        // Update Port
        panel.add(new JLabel("Update Port:"));
        updatePortField = new JTextField("12346");
        panel.add(updatePortField);

        // FFmpeg Command
        panel.add(new JLabel("FFmpeg Command: (%s for input and output)"));
        //Todo Här ser man att %s är en del av commandot för input och output..... Går nog att hitta en snyggare lösning
        ffmpegCommandField = new JTextField("ffmpeg -i %s -c:v libx264 -b:v 2000k -c:a aac -b:a 128k %s"); // Increased the columns to provide more visible space
        panel.add(ffmpegCommandField);

        panel.add(new JLabel("Output filename (will include identifier)"));
        customOutputFileName = new JTextField("");
        panel.add(customOutputFileName);


        JLabel filePathLabel = new JLabel("File name");
        panel.add(filePathLabel);

        JButton filePickerButton = new JButton("Pick File");
        JButton startButton = new JButton("Start");
        startButton.setEnabled(false); // Initially disable the button
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startTCPClient();
            }
        });

        filePickerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
                int returnValue = fileChooser.showOpenDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    filePath = fileChooser.getSelectedFile().getAbsolutePath();
                    filePathLabel.setText(fileChooser.getSelectedFile().getName());
                    customOutputFileName.setText(fileChooser.getSelectedFile().getName());
                    startButton.setEnabled(true); // Enable the button when a file is chosen
                }
            }
        });

        // Creating a south panel to hold both the startButton and filePickerButton
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new FlowLayout());
        southPanel.add(filePickerButton);
        southPanel.add(startButton);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(southPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void startTCPClient() {
        // Get values from text fields
        String serverAddress = serverAddressField.getText();
        int serverPort = Integer.parseInt(serverPortField.getText());
        int updatePort = Integer.parseInt(updatePortField.getText());
        String ffmpegCommand = ffmpegCommandField.getText();
        String outputFileName = customOutputFileName.getText();

        try {
            new TCPClient(serverAddress, serverPort, updatePort, filePath, ffmpegCommand, outputFileName);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error starting TCP Client: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TCPClientGUI();
            }
        });
    }
}