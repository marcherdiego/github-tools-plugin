package com.tal.android.plugin.github.ui.windows;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
}
