# Eventsdroid
Gradle plugin responsible for automatic analytics events classes generation based on a certain JSON-schema.

## How does it work?
Let's assume, you have analytics events JSON-schema, which has a certain pattern.
For now, this plugin works only with this one:

```JSON
[
  {
    "name": "programs",
    "events": [
      {
        "name": "trainings_list_view",
        "parameters": [
          {
            "name": "screen_name",
            "value": "trainings"
          },
          {
            "name": "program"
          }
        ]
      },
      {
        "name": "screen_view",
        "parameters": [
          {
            "name": "screen_name",
            "value": "trainings"
          }
        ]
      },
      {
        "name": "screen_view1",
        "parameters": [
          {
            "name": "result"
          },
          {
            "name": "screen_name",
            "value": "trainings"
          }
        ]
      }
    ]
  }
]
```

Basically, it represents list of different categories.
Each category contains list of events.
Each event consists of parameters (including the *required* one - `screen_name`), some of them may have already defined values, 
represented by `value` fields.

What it will generate, is ready-to-use data classes and objects each representing single event entry and 
deriving from `BaseEvent` entity (which itself will also be automatically generated):

```kotlin
open class BaseEvent(
        val categoryName: String,
        val screenName: String,
        val eventName: String,
        val params: Map<String, String>
)
```

All event data classes and objects are enclosed in the object, which represents the whole analytics category.

For the JSON above, this structure will look this way:

```kotlin
object ProgramsEvents {
    data class TrainingsListViewEvent(val program: String) : BaseEvent("programs", "trainings", "trainings_list_view", mapOf("program" to program))

    object ProgramsEvents : BaseEvent("programs", "trainings", "screen_view", emptyMap())

    data class ScreenView1Event(val result: String) : BaseEvent("programs", "trainings", "screen_view1", mapOf("result" to result))

    object Values {
        const val NULL: String = "null"
    }
}
```

### Q: In which case objects, and in which one data classes are generated?

Objects are generated for the events which require no custom parameters (except for screen_name, which is considered 
as required, and isn't optional).

Data classes are generated for the events with custom parameters, which are collected into custom parameters `Map<String, String>`.

### Q: What does `object Values { //... }` stand for?

It stands for predefined constant values which are accepted for different event parameters in the scope of this
events category.

## How to use

### 1. Apply gradle plugin.

