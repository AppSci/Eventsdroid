package com.betterme.eventsdroid

import com.betterme.eventsdroid.entities.AnalyticsCategoryEntity
import com.betterme.eventsdroid.entities.EventParameterEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File

class EventsGenerator(
        private val gson: Gson,
        private val destFilePath: File,
        private val packageName: String
) {

    companion object {
        private const val PARAM_SCREEN_NAME = "screen_name"

        private const val BASE_EVENT_CLASS_NAME = "BaseEvent"
    }

    fun generateEventsClasses(schemaFile: File) {
        val jsonString = readFile(schemaFile)

        val analyticsCategories = gson.fromJson<List<AnalyticsCategoryEntity>>(jsonString)
        analyticsCategories.forEach { eventEntity ->
            val categoryName = eventEntity.categoryName
            val formattedClassName = getFormattedEventSetClassName(categoryName)
            val className = ClassName("", formattedClassName)
            val rootObjectBuilder = TypeSpec.objectBuilder(formattedClassName)

            val predefinedValues = mutableSetOf<String>()

            eventEntity.events.forEach { event ->
                val eventName = event.eventName
                val eventClassName = getFormattedEventClassName(eventName)

                val parameters = event.parameters

                val screenParam = parameters.firstOrNull { parameter -> parameter.param == PARAM_SCREEN_NAME }
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
                val allPredefinedValuesForEvent = event.parameters.asSequence()
                        .filter { eventParam -> eventParam.param != PARAM_SCREEN_NAME }
                        .map { eventParam -> eventParam.value }

                predefinedValues.addAll(allPredefinedValuesForEvent)
            }

            val valuesObjectBuilder = TypeSpec.objectBuilder("Values")
            predefinedValues.forEach { value ->
                valuesObjectBuilder
                        .addProperty(PropertySpec.builder(getPredefinedValueVariableName(value), String::class)
                                .addModifiers(KModifier.CONST)
                                .initializer("%S", value)
                                .build())
            }

            rootObjectBuilder.addType(valuesObjectBuilder.build())

            val eventsFile = FileSpec.builder("$packageName.${categoryName.toLowerCase()}", "$className")
                    .addType(rootObjectBuilder.build())
                    .build()
            eventsFile.writeTo(destFilePath)
        }
    }

    fun generateBaseEventClass() {
        val baseEventClassBuilder = TypeSpec.classBuilder("BaseEvent").apply {
            primaryConstructor(FunSpec.constructorBuilder()
                    .addParameter("categoryName", String::class)
                    .addParameter("screenName", String::class)
                    .addParameter("eventName", String::class)
                    .addParameter("params", Map::class.parameterizedBy(String::class, String::class))
                    .build())
            addModifiers(KModifier.OPEN)
            addProperty(PropertySpec.builder("categoryName", String::class)
                    .initializer("categoryName")
                    .build())
            addProperty(PropertySpec.builder("screenName", String::class)
                    .initializer("screenName")
                    .build())
            addProperty(PropertySpec.builder("eventName", String::class)
                    .initializer("eventName")
                    .build())
            addProperty(PropertySpec.builder("params", Map::class.parameterizedBy(String::class, String::class))
                    .initializer("params")
                    .build()
            )
        }
        val file = FileSpec.builder(packageName, BASE_EVENT_CLASS_NAME)
                .addType(baseEventClassBuilder.build())
                .build()
        file.writeTo(destFilePath)
    }

    private fun createEventObjectBuilder(
            eventClassName: ClassName,
            categoryName: String,
            screenName: String,
            eventName: String
    ): TypeSpec.Builder {

        val eventObjectBuilder = TypeSpec.objectBuilder(eventClassName)

        return eventObjectBuilder
                .superclass(ClassName(packageName, BASE_EVENT_CLASS_NAME))
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
            if (index < parameters.size - 2) {
                // Separate all pairs except for the last one with comma.
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
                .superclass(ClassName(packageName, BASE_EVENT_CLASS_NAME))
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

    private fun getPredefinedValueVariableName(value: String) = value
            .toUpperCase().replace(" ", "_")

}
