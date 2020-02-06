package com.nerdscorner.android.plugin.android.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.nerdscorner.android.plugin.android.dialog.ModuleSelectorForm;

public class DependenciesComparator extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent actionEvent) {
        Project project = getEventProject(actionEvent);
        if (project == null || project.getBasePath() == null) {
            return;
        }
        ModuleSelectorForm moduleSelectorForm = new ModuleSelectorForm(project);
        moduleSelectorForm.pack();
        moduleSelectorForm.setLocationRelativeTo(null);
        moduleSelectorForm.setTitle("Select module to run dependencies comparison on");
        moduleSelectorForm.setResizable(true);
        moduleSelectorForm.setVisible(true);
    }

    @Override
    public boolean isDumbAware() {
        return false;
    }
}
