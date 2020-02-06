package com.nerdscorner.android.plugin.android.dialog;

import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.nerdscorner.android.plugin.android.commands.CommandExecutor;
import com.nerdscorner.android.plugin.utils.FileCreator;

public class ModuleSelectorForm extends JDialog {
    private JList<VirtualFile> modulesList;
    private JButton acceptButton;
    private JButton cancelButton;
    private JPanel contentPane;
    private Project project;

    public ModuleSelectorForm(Project project) {
        this.project = project;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(acceptButton);

        acceptButton.addActionListener(e -> onOK());

        cancelButton.addActionListener(e -> onCancel());

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        loadProjectModules();
    }

    private void loadProjectModules() {
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        VirtualFile[] sourceFolders = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile sourceFolder : sourceFolders) {
            model.addElement(sourceFolder);
        }
        this.modulesList.setModel(model);
    }

    private void onOK() {
        try {
            CommandExecutor commandExecutor = new CommandExecutor(project.getBasePath());
            String moduleName = "app";
            //Run dependencies for current branch
            String result = commandExecutor.execute("./gradlew " + moduleName + ":dependencies -q");
            FileCreator.INSTANCE.createFile(project.getBasePath(), "branch_dependencies.txt", result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onCancel() {
        dispose();
    }
}
