package com.nerdscorner.android.plugin.github.ui.windows;

import javax.swing.*;

public class ResultDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel resultMessage;

    public ResultDialog(String message) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.addActionListener(e -> dispose());
        resultMessage.setText(message);
    }

    public ResultDialog(String message, String okButtonText, Callback callback) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.setText(okButtonText);
        buttonOK.addActionListener(e -> {
            dispose();
            callback.onOk();
        });
        resultMessage.setText(message);
    }

    public interface Callback {
        void onOk();
    }
}
