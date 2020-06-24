package com.nerdscorner.android.plugin.github.ui.windows;

import javax.swing.*;

public class SimpleInputDialog extends JDialog {
    private JPanel contentPane;
    private JButton acceptButton;
    private JTextField input;
    private JButton cancelButton;

    public SimpleInputDialog(Callback callback) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(acceptButton);
        acceptButton.addActionListener(e -> {
            callback.onOk(input.getText());
        });
        cancelButton.addActionListener(e -> dispose());
    }

    public interface Callback {
        void onOk(String input);
    }
}
