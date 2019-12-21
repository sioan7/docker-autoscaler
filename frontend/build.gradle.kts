plugins {
    kotlin("jvm") version "1.3.60"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    val ktorVersion = "1.2.6"

    fun ktor(module: String) = "io.ktor:ktor-$module:$ktorVersion"
    fun ktor() = "io.ktor:ktor:$ktorVersion"

    implementation(kotlin("stdlib-jdk8"))
    compile(ktor())
    compile(ktor("server-netty"))
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.mongodb:mongodb-driver-sync:3.11.2")
    compile("com.rabbitmq:amqp-client:5.8.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}