package org.server;

import org.shared.SQliteManager;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * En klass som representerar ett fönster för att visa och hämta jobb från en databas.
 */
public class DatabaseWindow {
    private JFrame frame;
    private JButton getJobsButton;
    private JTextField ipTextField;
    private JTextArea textArea;
    private SQliteManager manager;

    /**
     * Konstruktor för DatabaseWindow-klassen.
     *
     * @param sqliteManager SQliteManager-objektet som används för att hantera databasen.
     */
    public DatabaseWindow(SQliteManager sqliteManager) {
        this.manager = sqliteManager;

        frame = new JFrame("Database Window");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        getJobsButton = new JButton("Get Jobs");
        ipTextField = new JTextField(15);
        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);

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

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("IP Address:"));
        topPanel.add(ipTextField);
        topPanel.add(getJobsButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Metod som visar fönstret för "databasen".
     */
    public void show() {
        frame.setVisible(true);
    }
}