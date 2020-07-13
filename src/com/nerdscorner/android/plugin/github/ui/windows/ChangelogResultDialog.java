package com.nerdscorner.android.plugin.github.ui.windows;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import javax.swing.*;

public class ChangelogResultDialog extends JDialog {
    private JButton copyButton;
    private JButton closeButton;
    private JLabel messageLabel;
    private JPanel contentPane;
    private JTextArea changelog;

    public ChangelogResultDialog(String message) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(copyButton);
        messageLabel.setVisible(false);
        changelog.setText(message);
        copyButton.addActionListener(e -> {
            StringSelection stringSelection = new StringSelection(message);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            messageLabel.setVisible(true);
        });
        closeButton.addActionListener(e -> dispose());
    }
}
