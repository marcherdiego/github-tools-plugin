package com.tal.android.plugin.commons;

import javax.swing.*;

public class ResultDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel resultMessage;

    public ResultDialog(String message) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        resultMessage.setText(message);
    }

    private void onOK() {
        dispose();
    }
}
