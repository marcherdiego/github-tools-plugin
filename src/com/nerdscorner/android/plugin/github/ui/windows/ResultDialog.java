package com.nerdscorner.android.plugin.github.ui.windows;

import javax.swing.*;

public class ResultDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel resultMessage;
    private JButton secondaryAction;

    public ResultDialog(String message) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.addActionListener(e -> dispose());
        resultMessage.setText(message);
        secondaryAction.setVisible(false);
    }

    public ResultDialog(String message, String okButtonText, SuccessCallback callback) {
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

    public ResultDialog(String message, String okButtonText, SuccessCallback success, String clearButtonText, ClearCallback clear) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.setText(okButtonText);
        buttonOK.addActionListener(e -> {
            dispose();
            success.onOk();
        });
        secondaryAction.setVisible(true);
        secondaryAction.setText(clearButtonText);
        secondaryAction.addActionListener(e -> {
            dispose();
            clear.onClear();
        });
        resultMessage.setText(message);
    }

    public interface SuccessCallback {
        void onOk();
    }

    public interface ClearCallback {
        void onClear();
    }
}
