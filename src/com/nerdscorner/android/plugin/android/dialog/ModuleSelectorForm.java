package com.nerdscorner.android.plugin.android.dialog;

import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.*;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.nerdscorner.android.plugin.android.commands.CommandExecutor;
import com.nerdscorner.android.plugin.utils.Constants;
import com.nerdscorner.android.plugin.utils.FileCreator;
import com.nerdscorner.android.plugin.utils.ProgressThread;

public class ModuleSelectorForm extends JDialog {
    private JList<String> modulesList;
    private JButton acceptButton;
    private JButton cancelButton;
    private JPanel panel_1;
    private JList<String> targetBranchList;
    private JList<String> sourceBranchList;
    private JLabel resultText;
    private Project project;

    private ProgressThread progressThread;

    public ModuleSelectorForm(Project project) {
        this.project = project;

        setContentPane(panel_1);
        setModal(true);
        getRootPane().setDefaultButton(acceptButton);
        progressThread = new ProgressThread();
        progressThread.setTarget(resultText);

        acceptButton.addActionListener(e -> onOK());

        cancelButton.addActionListener(e -> onCancel());

        // call onCancel() on ESCAPE
        panel_1.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        loadProjectModules();
        loadProjectBranches();
    }

    private void loadProjectModules() {
        DefaultListModel<String> model = new DefaultListModel<>();
        VirtualFile[] sourceFolders = ProjectRootManager.getInstance(project).getContentRoots();
        for (int i = 1; i < sourceFolders.length; i++) {
            model.addElement(sourceFolders[i].getName());
        }
        modulesList.setModel(model);
    }

    private void loadProjectBranches() {
        DefaultListModel<String> sourceBranchModel = new DefaultListModel<>();
        DefaultListModel<String> targetBranchModel = new DefaultListModel<>();
        CommandExecutor commandExecutor = new CommandExecutor(project.getBasePath());
        String gitBranchResponse = commandExecutor.execute("git branch");
        String[] branches = gitBranchResponse.split(Constants.NEW_LINE);
        for (String branch : branches) {
            String branchName = branch.replaceAll("\\*", Constants.BLANK).trim();
            sourceBranchModel.addElement(branchName);
            targetBranchModel.addElement(branchName);
        }
        sourceBranchList.setModel(sourceBranchModel);
        targetBranchList.setModel(targetBranchModel);
    }

    private void onOK() {
        new Thread(() -> {
            progressThread.start();
            progressThread.setMessage("Starting");
            try {
                String basePath = project.getBasePath();
                CommandExecutor commandExecutor = new CommandExecutor(basePath);
                String moduleName = modulesList.getSelectedValue();

                // Save original branch
                progressThread.setMessage("Saving original branch");
                String gitStatusResult = commandExecutor.execute("git status");
                String originalBranch = gitStatusResult
                        .split(Constants.NEW_LINE)[0]
                        .split(Constants.SPACE)[2]; //format is: `On branch <branch_name>`

                // Switch to source branch
                progressThread.setMessage("Switching to source branch");
                String sourceBranch = sourceBranchList.getSelectedValue();
                commandExecutor.execute("git checkout " + sourceBranch);

                // Run dependencies for source branch
                progressThread.setMessage("Running dependencies for source branch");
                String sourceBranchDependenciesResult = commandExecutor.execute("./gradlew " + moduleName + ":dependencies -q");
                FileCreator.INSTANCE.createFile(basePath, sourceBranch + "_dependencies.txt", sourceBranchDependenciesResult);

                // Switch to target branch
                progressThread.setMessage("Switching to target branch");
                String targetBranch = targetBranchList.getSelectedValue();
                commandExecutor.execute("git checkout " + targetBranch);

                // Run dependencies for target branch
                progressThread.setMessage("Running dependencies for target branch");
                String targetBranchDependenciesResult = commandExecutor.execute("./gradlew " + moduleName + ":dependencies -q");
                FileCreator.INSTANCE.createFile(basePath, targetBranch + "_dependencies.txt", targetBranchDependenciesResult);

                // Switch to original branch
                progressThread.setMessage("Returning original branch");
                commandExecutor.execute("git checkout " + originalBranch);

                onCancel();
            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> resultText.setText("FAILED! \n" + e.getMessage()));
            } finally {
                progressThread.stop();
            }
        }).start();
    }

    private void onCancel() {
        dispose();
    }
}
