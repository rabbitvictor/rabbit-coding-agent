# Rabbit Coding Agent

A demonstration of building a CLI Coding Agent in Kotlin using the official Google Gemini Java SDK.

## Overview

This project showcases how to build an intelligent CLI agent that leverages Google's Gemini API to understand and
execute coding tasks. It demonstrates the integration of Kotlin with the Gemini Java SDK, including:

## Features

- Direct integration with Gemini 2.5 Flash model
- Custom tool registration using Java reflection
- Function calling capabilities for tool invocation
- CLI interface for interaction

## Getting Started

### Prerequisites

- Java 21 or later
- Gradle (included via Gradle wrapper)
- Google Gemini API key

### Building

```bash
./gradlew build
```

This creates a fat JAR with the ShadowJar plugin:

```bash
./gradlew shadowJar
```

### Running

Set your Gemini API key:

```bash
export GOOGLE_API_KEY=your-api-key-here
```

Run the application:

```bash
java -jar build/libs/rabbit-coding-agent-0.0.1-SNAPSHOT.jar
```

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
