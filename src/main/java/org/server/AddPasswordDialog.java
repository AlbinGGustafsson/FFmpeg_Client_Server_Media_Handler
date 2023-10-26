package org.server;

import javax.swing.*;
import java.awt.*;

public class AddPasswordDialog {

    private JDialog passwordDialog;
    private JTextField ipField;
    private JPasswordField passwordField;
    private boolean isConfirmed = false;

    public AddPasswordDialog(JFrame parent) {
        passwordDialog = new JDialog(parent, "Add Password", true);
        passwordDialog.setSize(300, 150);
        passwordDialog.setLocationRelativeTo(parent);
        passwordDialog.setLayout(new GridLayout(3, 2));

        JLabel ipLabel = new JLabel("IP Address:");
        ipField = new JTextField();

        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField();

        JButton addButton = new JButton("Add");
        addButton.addActionListener(ae -> {
            isConfirmed = true;
            passwordDialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(ae -> {
            passwordDialog.dispose();
        });

        passwordDialog.add(ipLabel);
        passwordDialog.add(ipField);
        passwordDialog.add(passwordLabel);
        passwordDialog.add(passwordField);
        passwordDialog.add(addButton);
        passwordDialog.add(cancelButton);
    }

    public void display() {
        passwordDialog.setVisible(true);
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public String getIpAddress() {
        return ipField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }
}