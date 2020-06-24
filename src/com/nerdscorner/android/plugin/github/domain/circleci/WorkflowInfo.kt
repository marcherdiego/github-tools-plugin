package com.nerdscorner.android.plugin.github.domain.circleci

import gherkin.deps.com.google.gson.annotations.SerializedName
import java.io.Serializable

class WorkflowInfo : Serializable {
    @SerializedName("workflow-id")
    var workflowId: String? = null

    @SerializedName("actor-id")
    var actorId: String? = null
}
