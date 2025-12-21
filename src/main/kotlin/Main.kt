package com.rabbitvictor

import com.google.genai.Client
import com.google.genai.types.AutomaticFunctionCallingConfig
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Tool
import com.google.genai.types.ToolConfig
import java.lang.reflect.Method


fun main() {
    callGemini()
}


object WeatherTool {
    @JvmStatic
    fun getCurrentWeather(location: String, unit: String?) = "The weather in $location is very nice."
}


fun callGemini() {
    val client = Client()
    val weatherTool = WeatherTool::class.java.getMethod("getCurrentWeather", String::class.java, String::class.java)
    val toolConfig = Tool.builder().functions(weatherTool).build()

    val generateContentConfig = GenerateContentConfig
        .builder()
        .automaticFunctionCalling(AutomaticFunctionCallingConfig.builder().disable(true))
        .tools(toolConfig).build()

    val response = client
        .models
        .generateContent(
            "gemini-2.5-flash",
            "How is the weather in São Paulo right now in Celsius?",
            generateContentConfig
        )

    response.functionCalls()?.forEach { call ->
        println("called ${call.name()}")
    }

    println(response.text())
}


//object GenerateContentWithFunctionCall {
//    fun getCurrentWeather(location: String, unit: String?): String {
//        return "The weather in " + location + " is " + "very nice."
//    }
//
//    @Throws(NoSuchMethodException::class)
//    @JvmStatic
//    fun main(args: Array<String>) {
//        val client = Client()
//
//        // Load the method as a reflected Method object so that it can be
//        // automatically executed on the client side.
//        val method: Method? =
//            GenerateContentWithFunctionCall::class.java.getMethod(
//                "getCurrentWeather", String::class.java, String::class.java
//            )
//
//        val config: GenerateContentConfig? =
//            GenerateContentConfig.builder()
//                .tools(Tool.builder().functions(method))
//                .build()
//
//        val response =
//            client.models.generateContent(
//                "gemini-2.5-flash",
//                "What is the weather in Vancouver?",
//                config
//            )
//
//        println("The response is: " + response.text())
//        println(
//            "The automatic function calling history is: "
//                + response.automaticFunctionCallingHistory().get()
//        )
//    }
//}