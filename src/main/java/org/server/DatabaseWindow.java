package org.server;

import org.shared.SQliteManager;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseWindow {
    private JFrame frame;
    private JButton getJobsButton;
    private JTextField ipTextField;
    private JTextArea textArea;
    private SQliteManager manager;

    public DatabaseWindow(SQliteManager sqliteManager) {
        this.manager = sqliteManager;

        frame = new JFrame("Database Window");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create components
        getJobsButton = new JButton("Get Jobs");
        ipTextField = new JTextField(15);
        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

        // Set up action for the button
        getJobsButton.addActionListener(e -> {
            textArea.append("Fetched jobs for IP: " + ipTextField.getText() + "\n");
            ArrayList<String> jobs;
            try {
                if (ipTextField.getText().equals("")){
                    jobs = new ArrayList<>(manager.getJobs());
                }else{
                    jobs = new ArrayList<>(manager.getJobs(ipTextField.getText()));
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            for (String job : jobs) {
                textArea.append(job + "\n");
            }
        });

        // Create a panel for the top components
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("IP Address:"));
        topPanel.add(ipTextField);
        topPanel.add(getJobsButton);

        // Add components to the frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
    }

    public void show() {
        frame.setVisible(true);
    }
}