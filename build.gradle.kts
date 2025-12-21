plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

group = "com.rabbitvictor"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.google.genai)
    
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

kotlin {
    jvmToolchain(24)
    compilerOptions {
        allWarningsAsErrors.set(true)
        javaParameters.set(true)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "com.rabbitvictor.MainKt"
        )
    }
    archiveClassifier.set("")
}

tasks.named("build") {
    dependsOn("shadowJar")
}