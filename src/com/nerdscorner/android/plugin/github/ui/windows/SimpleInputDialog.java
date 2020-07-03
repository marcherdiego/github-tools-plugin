package com.nerdscorner.android.plugin.github.ui.windows;

import javax.swing.*;

public class SimpleInputDialog extends JDialog {
    private JPanel contentPane;
    private JButton acceptButton;
    private JTextField input;
    private JButton cancelButton;
    private JLabel message;

    public SimpleInputDialog(Callback callback, String message) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(acceptButton);
        this.message.setText(message);
        acceptButton.addActionListener(e -> {
            dispose();
            callback.onOk(input.getText());
        });
        cancelButton.addActionListener(e -> dispose());
    }

    public interface Callback {
        void onOk(String input);
    }
}
