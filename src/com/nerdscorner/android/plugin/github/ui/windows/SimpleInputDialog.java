package com.nerdscorner.android.plugin.github.ui.windows;

import org.greenrobot.eventbus.EventBus;

import javax.swing.*;

public class SimpleInputDialog extends JDialog {
    private JPanel contentPane;
    private JButton acceptButton;
    private JTextField input;
    private JButton cancelButton;
    private JLabel message;

    public SimpleInputDialog(String message, int requestCode, EventBus bus) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(acceptButton);
        this.message.setText(message);
        acceptButton.addActionListener(e -> {
            dispose();
            bus.post(new InputEnteredEvent(input.getText(), requestCode));
        });
        cancelButton.addActionListener(e -> dispose());
    }

    public static class InputEnteredEvent {
        public final String text;
        public final int requestCode;

        InputEnteredEvent(String text, int requestCode) {
            this.text = text;
            this.requestCode = requestCode;
        }
    }
}
