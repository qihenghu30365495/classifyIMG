package org.imgCheckout;

import org.imgCheckout.gui.ImageOrganizerGUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ImageOrganizerGUI app = new ImageOrganizerGUI();
            app.setVisible(true);
        });
    }
}    