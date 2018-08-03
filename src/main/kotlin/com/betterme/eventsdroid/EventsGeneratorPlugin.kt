package com.betterme.eventsdroid

import org.gradle.api.Plugin
import org.gradle.api.Project

class EventsGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.create("generateEvents", EventsGeneratorTask::class.java)
    }

}