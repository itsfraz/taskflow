package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class SmartSuggestion(
    val type: String,       // "new" or "optimize"
    val title: String,      // Recommended task title or existing task title matching target
    val description: String,// Why recommended, or optimized bullet points
    val category: String,   // Category suggestion
    val priority: String    // Priority suggestion
)

data class ParsedTaskResult(
    val title: String,
    val description: String,
    val priority: String, // "Low", "Medium", "High", "Urgent"
    val category: String, // e.g. "Work", "Personal", "Health", "Shopping"
    val dueDateMs: Long?, // Epoch millis for due date, or null
    val dueTime: String?, // string "HH:mm" like "20:00" or null
    val reminderTimeMs: Long?, // Epoch millis for reminder time, or null
    val repeatOption: String // "Daily", "Weekly", "Monthly", "None"
)

object SmartSuggestionHelper {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Parse natural language statement into a structured Task model using Gemini AI
    suspend fun parseNaturalLanguageTask(inputText: String, currentLocalDateDesc: String, currentEpochMs: Long): ParsedTaskResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key is missing or default. Please configure GEMINI_API_KEY in the Secrets panel.")
        }

        val prompt = """
            You are a world-class Natural Language Parser designed for productivity powerhouses like Todoist.
            Your job is to read an unstructured task instruction and map it to a structured JSON object.

            Current Date Reference for calculations:
            - Absolute String: $currentLocalDateDesc
            - Reference Epoch Milliseconds: $currentEpochMs

            Analyze the user prompt: "$inputText"

            Extract the following parameters:
            - "title": String. The main core command or task action. (e.g. "Submit my assignment", "Call John", "Buy groceries")
            - "description": String. Any additional context, place terms, or empty string.
            - "priority": String. Must be exactly one of "Low", "Medium", "High", "Urgent". If not mentioned, determine based on tone or default to "Medium".
            - "category": String. Choose from standard domains: "Work", "Personal", "Health", "Shopping", "Learning", "Finance".
            - "dueDateMs": Long or null. Calculate the exact epoch Milliseconds at midnight (start of day) of the extracted due date. e.g. if prompt says 'tomorrow', add 1 day of milliseconds to reference epoch. If prompt says 'every Monday', return the next Monday starting day.
            - "dueTime": String or null. Format matching "HH:mm" (e.g. "20:00", "08:30") if they specific a time like "8 PM" or "at 10 AM".
            - "reminderTimeMs": Long or null. If reminder is specified, calculate the exact corresponding epoch milliseconds. E.g. if prompt specifies a specific time/date, calculate that exact epoch.
            - "repeatOption": String. Must be exactly one of "Daily", "Weekly", "Monthly", "None". For example, "every Monday" -> "Weekly", "every day" -> "Daily".

            Response format constraint:
            Return ONLY raw valid JSON matching this schema. Do not wrap in backticks or Markdown blocks.
            Example output format:
            {"title":"Submit assignment","description":"Group task","priority":"High","category":"Work","dueDateMs":1781980800000,"dueTime":"20:00","reminderTimeMs":1781980800000,"repeatOption":"None"}
        """.trimIndent()

        val request = GeminiPromptRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        val response = GeminiRetrofitClient.endpoint.generateContent(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty assistant parser response")

        return parseNaturalLanguageResultJson(textResponse)
    }

    private fun parseNaturalLanguageResultJson(raw: String): ParsedTaskResult {
        Log.d("SmartSuggestionHelper", "Raw AI Parse response: $raw")
        val clean = raw.trim()
        val jsonStartIdx = clean.indexOf('{')
        val jsonEndIdx = clean.lastIndexOf('}')

        if (jsonStartIdx == -1 || jsonEndIdx == -1) {
            throw IllegalStateException("Failed to parse AI structure from output")
        }

        val parsedJson = clean.substring(jsonStartIdx, jsonEndIdx + 1)
        val adapter = moshi.adapter(ParsedTaskResult::class.java)
        return adapter.fromJson(parsedJson) ?: throw IllegalStateException("Returned json model empty")
    }

    // Helper to request suggestions from Gemini based on current tasks
    suspend fun getSmartSuggestions(tasks: List<TaskEntity>): List<SmartSuggestion> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key is missing or default. Please configure GEMINI_API_KEY in the Secrets panel.")
        }

        val tasksContext = if (tasks.isEmpty()) {
            "The user currently has no tasks scheduled. Suggest some foundational productivity start-up tasks."
        } else {
            "Here is the user's current task list:\n" + tasks.joinToString("\n") { task ->
                "- Title: \"${task.title}\" | Category: \"${task.category}\" | Priority: \"${task.priority}\" | Status: ${if (task.isCompleted) "Completed" else "Pending"} | Description: \"${task.description}\""
            }
        }

        val prompt = """
            You are an elite, highly intelligent productivity coach and task assistant.
            You must analyze the user's current task list and provide up to 5 strategic suggestions.
            Suggestions should aim to increase productivity, recommend logical next-action steps as new tasks, or optimize/expand existing verbose pending tasks into step-by-step subtask checklists.

            $tasksContext

            Provide your recommendations strictly as a JSON array of objects.
            Each object MUST have the following fields:
            - "type": String. Must be exactly either "new" (for proposing a new related/logical next task) or "optimize" (for proposing step-by-step micro-checklist details to optimize/expand an existing pending task).
            - "title": String. For type "new", this is the new related task title. For type "optimize", this MUST match the existing task's title exactly.
            - "description": String. For type "new", explain briefly why you suggest this task. For type "optimize", provide a clean, detailed list of subtask items (format with hyphens/bullets, e.g. "- Step 1\n- Step 2") to accomplish that task.
            - "category": String. Use simple categories matching standard domains (e.g. "Work", "Personal", "Health", "Shopping", "Learning").
            - "priority": String. Suggested priority (e.g. "Low", "Medium", "High", "Urgent").

            Return ONLY raw valid JSON matching this schema. Do not enclose the JSON in markdown formatting (like ```json ... ```) or any preamble/postamble.
        """.trimIndent()

        val request = GeminiPromptRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        val response = GeminiRetrofitClient.endpoint.generateContent(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty response from AI engine")

        return parseSuggestionsJson(textResponse)
    }

    // Helper to generate an optimization plan specifically for a single task
    suspend fun optimizeTaskDescription(task: TaskEntity): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key is missing. Configure GEMINI_API_KEY in Secrets panel.")
        }

        val prompt = """
            You are an elite task strategist. Read this task currently labeled:
            Title: "${task.title}"
            Current Description: "${task.description}"
            Category: "${task.category}"
            Priority: "${task.priority}"

            Generate an optimized, highly actionable, step-by-step micro-checklist to complete this task successfully.
            Format it as a clean bulleted list (using hyphens) with clear, practical steps. Keep the response concise, punchy, and professional. 
            Do NOT include introductions, preambles, or conversational greetings. Start directly with the bullet points.
        """.trimIndent()

        val request = GeminiPromptRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        val response = GeminiRetrofitClient.endpoint.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("No optimization response received")
    }

    // Extract valid JSON substring and parse
    private fun parseSuggestionsJson(raw: String): List<SmartSuggestion> {
        Log.d("SmartSuggestionHelper", "Raw AI response: $raw")
        val clean = raw.trim()
        val jsonStartIdx = clean.indexOf('[')
        val jsonEndIdx = clean.lastIndexOf(']')

        if (jsonStartIdx == -1 || jsonEndIdx == -1) {
            throw IllegalStateException("Failed to parse AI output: Could not find valid JSON arrays")
        }

        val parsedJson = clean.substring(jsonStartIdx, jsonEndIdx + 1)
        
        val listType = Types.newParameterizedType(List::class.java, SmartSuggestion::class.java)
        val adapter = moshi.adapter<List<SmartSuggestion>>(listType)
        return adapter.fromJson(parsedJson) ?: emptyList()
    }
}
