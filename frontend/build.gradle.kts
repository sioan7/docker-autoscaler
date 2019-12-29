import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:5.2.0")
    }
}

plugins {
    kotlin("jvm") version "1.3.60"
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "nl.leidenuniv"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "nl.leidenuniv.ApplicationKt"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    val ktorVersion = "1.2.6"

    fun ktor(module: String) = "io.ktor:ktor-$module:$ktorVersion"
    fun ktor() = "io.ktor:ktor:$ktorVersion"

    implementation(kotlin("stdlib-jdk8"))
    implementation(ktor("html-builder"))
    implementation(ktor("gson"))
    compile(ktor())
    compile(ktor("server-netty"))
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.mongodb:mongodb-driver-sync:3.11.2")
    compile("com.rabbitmq:amqp-client:5.8.0")
    compile("org.mongodb:mongodb-driver-sync:3.12.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<ShadowJar> {
        baseName = "frontend"
        classifier = null
        version = null
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}