package com.betterme.eventsdroid

import com.betterme.eventsdroid.entities.AnalyticsCategoryEntity
import com.betterme.eventsdroid.entities.Event
import com.betterme.eventsdroid.entities.EventParameterEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.kotlinpoet.*
import java.io.File

class EventsGenerator(
        private val gson: Gson,
        private val destFilePath: File,
        private val packageName: String
) {

    companion object {
        private const val PARAM_SCREEN_NAME = "screen_name"
    }

    fun generateEventsClasses(schemaFile: File) {
        val jsonString = readFile(schemaFile)

        val analyticsCategories = gson.fromJson<List<AnalyticsCategoryEntity>>(jsonString)
        analyticsCategories.forEach {
            val categoryName = it.categoryName
            val formattedClassName = getFormattedEventSetClassName(categoryName)
            val className = ClassName("", formattedClassName)
            val rootObjectBuilder = TypeSpec.objectBuilder(formattedClassName)

            val predefinedValues = mutableSetOf<String>()

            it.events.forEach {
                val eventName = it.eventName
                val eventClassName = getFormattedEventClassName(eventName)

                val parameters = it.parameters

                val screenParam = parameters.firstOrNull { it.param == PARAM_SCREEN_NAME }
                        ?: throw IllegalStateException("screen_name parameter is not defined!")
                val screenName = screenParam.value

                if (parameters.size > 1) {
                    // If there are many parameters defined, data class with custom fields will be generated.
                    val eventDataClassBuilder = createEventDataClassBuilder(parameters,
                            eventClassName, categoryName, screenName, eventName)

                    rootObjectBuilder.addType(eventDataClassBuilder.build())
                } else {
                    // Otherwise, plain Event object with empty custom parameters map will be
                    // generated for this event.
                    val eventObjectBuilder = createEventObjectBuilder(className, categoryName,
                            screenName, eventName)

                    rootObjectBuilder.addType(eventObjectBuilder.build())
                }

                // Add all predefined values
                val allPredefinedValuesForEvent = it.parameters.asSequence()
                        .filter { it.param != PARAM_SCREEN_NAME }
                        .map { it.value }

                predefinedValues.addAll(allPredefinedValuesForEvent)
            }


            val valuesObjectBuilder = TypeSpec.objectBuilder("Values")
            predefinedValues.forEach {
                valuesObjectBuilder
                        .addProperty(PropertySpec.builder(getVariableName(it), String::class)
                                .addModifiers(KModifier.CONST)
                                .initializer("%S", it)
                                .build())
            }

            rootObjectBuilder.addType(valuesObjectBuilder.build())

            val file = FileSpec.builder("$packageName.$categoryName", "$className")
                    .addType(rootObjectBuilder.build())
                    .build()
            file.writeTo(destFilePath)
        }
    }

    private fun createEventObjectBuilder(
            eventClassName: ClassName,
            categoryName: String,
            screenName: String,
            eventName: String
    ): TypeSpec.Builder {

        val eventObjectBuilder = TypeSpec.objectBuilder(eventClassName)

        return eventObjectBuilder
                .superclass(Event::class)
                .addSuperclassConstructorParameter("%S", categoryName)
                .addSuperclassConstructorParameter("%S", screenName)
                .addSuperclassConstructorParameter("%S", eventName)
                .addSuperclassConstructorParameter("emptyMap()")
    }

    private fun createEventDataClassBuilder(
            parameters: List<EventParameterEntity>,
            eventClassName: String,
            categoryName: String,
            screenName: String,
            eventName: String
    ): TypeSpec.Builder {

        val eventDataClassBuilder = TypeSpec.classBuilder(eventClassName)

        val eventParams = parameters.filter { it.param != PARAM_SCREEN_NAME }

        // Generate map of custom parameters
        val customEventParamsBuilder = CodeBlock.builder()
        customEventParamsBuilder.add("mapOf(")
        eventParams.forEachIndexed { index, field ->
            customEventParamsBuilder.add("%S to %S", field.param, field.value)
            if (index < parameters.size - 1) {
                customEventParamsBuilder.add(", ")
            }
        }
        customEventParamsBuilder.add(")")

        val constructorBuilder = FunSpec.constructorBuilder()

        eventParams.forEach {
            constructorBuilder.addParameter(it.param, String::class)

            eventDataClassBuilder.addProperty(PropertySpec.builder(it.param, String::class)
                    .initializer(it.param)
                    .build())
        }

        return eventDataClassBuilder
                .addModifiers(KModifier.DATA)
                .primaryConstructor(constructorBuilder.build())
                .superclass(Event::class)
                .addSuperclassConstructorParameter("%S", categoryName)
                .addSuperclassConstructorParameter("%S", screenName)
                .addSuperclassConstructorParameter("%S", eventName)
                .addSuperclassConstructorParameter(customEventParamsBuilder.build())
    }

    private fun readFile(file: File): String = file.readText(Charsets.UTF_8)

    private fun getFormattedEventSetClassName(categoryName: String): String {
        return categoryName.capitalize().plus("Events")
    }

    private fun getFormattedEventClassName(eventName: String): String {
        return eventName
                .split("_")
                .asSequence()
                .map { it.capitalize() }
                .joinToString(separator = "", postfix = "Event")
    }

    inline fun <reified T> Gson.fromJson(json: String) =
            this.fromJson<T>(json, object: TypeToken<T>() {}.type)

    private fun getVariableName(field: String) = field
            .toUpperCase().replace(" ", "_")

}
