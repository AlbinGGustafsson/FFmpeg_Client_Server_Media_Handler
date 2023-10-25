import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ServerGUI extends JFrame {

    private JTextField filePortField;
    private JTextField updatePortField;
    private JButton startButton;
    private DefaultListModel<ConnectionInfo> listModel;
    private JList<ConnectionInfo> connectionList;
    private JTextArea logArea;

    public ServerGUI() {
        // Set up the frame
        setTitle("TCP Server");
        setSize(600, 300);
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

        // Set up the layout
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
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

    private class StartServerAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int filePort = Integer.parseInt(filePortField.getText());
            int updatePort = Integer.parseInt(updatePortField.getText());

            // Start the server in a new thread to keep the GUI responsive
            new Thread(() -> {
                try {
                    logMessage("Server started on file port " + filePort + " and update port " + updatePort);
                    TCPServer.startServer(filePort, updatePort, ServerGUI.this);
                } catch (IOException ex) {
                    logMessage("Failed to start server: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    public void addConnection(ConnectionInfo connectionInfo) {
        listModel.addElement(connectionInfo);
        logMessage(connectionInfo + " Started!");
    }

    public void removeConnection(ConnectionInfo connectionInfo, String message) {
        if (listModel.removeElement(connectionInfo)){
            logMessage(connectionInfo + message);
        }
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public static void main(String[] args) {
        new ServerGUI();
    }
}