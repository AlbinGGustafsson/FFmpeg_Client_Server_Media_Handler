package org.server;

import javax.swing.*;
import java.awt.*;

/**
 * En dialogrutaklass som används för att lägga till ett lösenord till en ip-adress.
 */
public class AddPasswordDialog {

    private JDialog passwordDialog;
    private JTextField ipField;
    private JPasswordField passwordField;
    private boolean isConfirmed = false;

    /**
     * Konstruktor för AddPasswordDialog-klassen.
     *
     * @param parent parent fönstret till dialogen.
     */
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

    /**
     * Metod som visar dialogrutan.
     */
    public void display() {
        passwordDialog.setVisible(true);
    }

    /**
     * Metod som returnerar true om användaren har bekräftat dialogrutan, annars false.
     *
     * @return true om användaren har bekräftat dialogrutan, annars false.
     */
    public boolean isConfirmed() {
        return isConfirmed;
    }

    /**
     * Metod som returnerar den angivna IP-adressen.
     *
     * @return IP-adressen som angivits av användaren.
     */
    public String getIpAddress() {
        return ipField.getText();
    }

    /**
     * Metod som returnerar det angivna lösenordet.
     *
     * @return Lösenordet som angivits av användaren.
     */
    public String getPassword() {
        return new String(passwordField.getPassword());
    }
}