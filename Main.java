package app;

import ui.LoginFrame;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Swing screens must be created on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}