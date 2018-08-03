package com.betterme.eventsdroid

import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

open class EventsGeneratorTask : DefaultTask() {

    @Input val eventsSchemaFile = project.objects.property(File::class.java)
    @Input val destPath = project.objects.property(File::class.java)
    @Input val packageName = project.objects.property(String::class.java)

    init {
        description = "Generates analytics events classes"
        group = "eventsdroid"
    }

    @TaskAction
    fun generateEvents() {
        val eventsGenerator = EventsGenerator(Gson(), destPath.get(), packageName.get())
        eventsGenerator.generateEventsClasses(eventsSchemaFile.get())
    }
}