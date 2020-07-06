package com.nerdscorner.android.plugin.github.ui.windows;

import org.greenrobot.eventbus.EventBus;

import javax.annotation.Nullable;
import javax.swing.*;

public class ResultDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel resultMessage;
    private JButton secondaryAction;

    private int requestCode;

    ResultDialog(String message) {
        this(message, null, null);
    }

    public ResultDialog(String message, String primaryButtonText, final EventBus bus) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.setText(primaryButtonText);
        buttonOK.addActionListener(e -> {
            dispose();
            bus.post(new PrimaryButtonClickedEvent(requestCode));
        });
        secondaryAction.addActionListener(e -> {
            dispose();
            bus.post(new SecondaryButtonClickedEvent(requestCode));
        });
        secondaryAction.setVisible(false);
        resultMessage.setText(message);
    }

    public void updateLoadingDialog(@Nullable String title, @Nullable String message, String primaryActionText,
                                    @Nullable String secondaryActionText, int requestCode) {
        if (title != null) {
            setTitle(title);
        }
        buttonOK.setText(primaryActionText);
        secondaryAction.setVisible(secondaryActionText != null);
        secondaryAction.setText(secondaryActionText);
        this.requestCode = requestCode;
        if (message != null) {
            resultMessage.setText(message);
        }
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public static class PrimaryButtonClickedEvent {
        public int requestCode;

        PrimaryButtonClickedEvent(int requestCode) {
            this.requestCode = requestCode;
        }
    }

    public static class SecondaryButtonClickedEvent {
        public int requestCode;

        SecondaryButtonClickedEvent(int requestCode) {
            this.requestCode = requestCode;
        }
    }
}
