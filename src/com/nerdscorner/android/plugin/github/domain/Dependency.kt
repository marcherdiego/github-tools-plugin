package com.nerdscorner.android.plugin.github.domain

import javax.swing.JButton

class Dependency {
    var id: String? = null
    var name: String? = null
    var level: Int = 0
    var url: String? = null
    var dependsOn: List<Dependency>? = null
    var dependsOnIds: String? = null

    var widget: JButton? = null

    constructor(id: String, name: String, level: Int, url: String, dependsOnIds: String?) {
        this.id = id
        this.name = name
        this.level = level
        this.url = url
        this.dependsOnIds = dependsOnIds
    }

    constructor(id: String, name: String, level: Int, url: String, dependsOn: List<Dependency>) {
        this.id = id
        this.name = name
        this.level = level
        this.url = url
        this.dependsOn = dependsOn
    }
}
