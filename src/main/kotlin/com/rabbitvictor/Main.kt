package com.rabbitvictor

import com.google.genai.Client
import com.google.genai.types.AutomaticFunctionCallingConfig
import com.google.genai.types.Content
import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import com.google.genai.types.Schema
import com.google.genai.types.Tool
import com.google.genai.types.Type
import java.io.File


fun main() {
    // Initialize Gemini client and configure tools
    val client = Client()
    val toolConfig = buildTools()
    val generateContentConfig = GenerateContentConfig
        .builder()
        .automaticFunctionCalling(AutomaticFunctionCallingConfig.builder().disable(true))
        .tools(toolConfig).build()

    // Create a chat session with Rabbit model
    val chatSession = client.chats.create("gemini-3-flash-preview", generateContentConfig)
    var toolCallResult = ""
    var readUserInput = true

    // Maintain conversation history for multi-turn interaction
    val conversation = mutableListOf<Content>()

    while (true) {
        // Read user input if needed and add to conversation history
        if (readUserInput) {
            print("\u001b[94mYou\u001b[0m: ")
            val userInput = readlnOrNull() ?: ""
            conversation.addMessage(userInput)
        }

        // Send conversation to LLM call and check if response contains tool calls
        val response = chatSession.sendMessage(conversation)
        val hasToolCall = response.functionCalls()?.isNotEmpty() ?: false

        when (hasToolCall) {
            // If LLM call wants to call a tool: execute it and add result back to conversation
            true -> {
                toolCallResult = toolCall(response)
                conversation.addMessage(toolCallResult)
            }

            // If LLM call has final response: print it and wait for next user input
            false -> {
                println("\u001b[91mRabbit\u001b[0m: ${response.text()}")
                readUserInput = true
                continue
            }
        }

        // Loop again without reading user input to process tool result
        readUserInput = false
    }
}

fun listFiles(path: String): List<String> {
    val paths = File(path).listFiles()
        ?.mapNotNull {
            if (it.isDirectory) {
                "/${it.name}"
            } else {
                it.name
            }
        } ?: emptyList()
    return paths.also {
        println("\u001b[92mTool\u001b[0m: listFiles($path)")
    }
}

fun readFile(path: String): String {
    val file = File(path)
    if (!file.isFile) return "path $path is not a file"
    return file.readText().also {
        println("\u001b[92mTool\u001b[0m: readFile($path)")
    }
}

fun editFile(path: String, oldText: String, newText: String): String {
    val file = File(path)

    // If file doesn't exist, create it with newText
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        file.writeText(newText)
        return "File $path created successfully".also {
            println("\u001b[92mTool\u001b[0m: editFile($path)")
        }
    }

    if (!file.isFile) return "path $path is not a file".also {
       println("\u001b[92mTool\u001b[0m: editFile($path)")
    }

    val content = file.readText()
    if (!content.contains(oldText)) return "oldText not found in file $path"
    val updatedContent = content.replace(oldText, newText)
    file.writeText(updatedContent)
    return "File $path edited successfully".also {
        println("\u001b[92mTool\u001b[0m: editFile($path)")
    }
}

private fun MutableList<Content>.addMessage(text: String): Boolean = add(
    Content.builder()
        .role("user")
        .parts(Part.fromText(text))
        .build()
)

private fun toolCall(response: GenerateContentResponse?): String {
    return response?.functionCalls()?.map { call ->
        when (val toolName = call.name().get()) {
            "listFiles" -> {
                val path = call.args().get().getValue("path") as? String ?: "."
                listFiles(
                    path = path,
                )
            }

            "readFile" -> {
                val path = call.args().get().getValue("path") as? String ?: "."
                readFile(
                    path = path,
                )
            }

            "editFile" -> {
                val path = call.args().get().getValue("path") as? String ?: "."
                val oldText = call.args().get().getValue("oldText") as? String ?: ""
                val newText = call.args().get().getValue("newText") as? String ?: ""
                editFile(
                    path = path,
                    oldText = oldText,
                    newText = newText,
                )
            }

            else -> {
                "unknown tool call with name = $toolName"
            }
        }
    }?.joinToString() ?: ""
}

private fun buildStringSchema(description: String): Schema =
    Schema.builder().apply {
        type(Type.Known.STRING)
        description(description)
    }.build()

private fun buildObjectSchema(properties: Map<String, Schema>, requiredFields: List<String>): Schema =
    Schema.builder().apply {
        type(Type.Known.OBJECT)
        properties(properties)
        required(*requiredFields.toTypedArray())
    }.build()

private fun buildFunctionDeclaration(
    name: String,
    description: String,
    parameters: Schema
): FunctionDeclaration =
    FunctionDeclaration.builder().apply {
        name(name)
        description(description)
        parameters(parameters)
    }.build()

private fun buildTools(): Tool {
    val listFilesParams = buildObjectSchema(
        properties = mapOf("path" to buildStringSchema("path to directory that should be have its files listed")),
        requiredFields = listOf("path")
    )
    val listFilesTool = buildFunctionDeclaration(
        name = "listFiles",
        description = "list files in current directory path. If the path is not given, list files in the current path",
        parameters = listFilesParams
    )

    val readFileParams = buildObjectSchema(
        properties = mapOf("path" to buildStringSchema("path to file that should be read")),
        requiredFields = listOf("path")
    )
    val readFileTool = buildFunctionDeclaration(
        name = "readFile",
        description = "reads a file at the given path. If the path is not a valid file, returns a message explaining it.",
        parameters = readFileParams
    )

    val editFileParams = buildObjectSchema(
        properties = mapOf(
            "path" to buildStringSchema("path to file that should be edited"),
            "oldText" to buildStringSchema("text to find and replace in the file"),
            "newText" to buildStringSchema("new text to replace with")
        ),
        requiredFields = listOf("path", "oldText", "newText")
    )
    val editFileTool = buildFunctionDeclaration(
        name = "editFile",
        description = "Makes edits to a file by replacing oldText with newText. If the file does not exist, creates it with newText as content.",
        parameters = editFileParams
    )

    return Tool
        .builder()
        .functionDeclarations(listFilesTool, readFileTool, editFileTool)
        .build()
}