import com.google.protobuf.gradle.*

val grpcVersion = "1.18.0"
val protobufVersion = "3.7.1"
val protocVersion = "3.7.1"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.8")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.41"
    id("org.jetbrains.dokka") version "0.9.17"
    id("com.google.protobuf") version "0.8.8"

    `java-library`
    `maven-publish`
}

group = "org.kerala"
version = "0.0.1"

repositories {
    jcenter()
}

dependencies {
    compile("com.google.api.grpc:proto-google-common-protos:1.0.0")
    compile("io.grpc:grpc-alts:${grpcVersion}")
    compile("io.grpc:grpc-netty-shaded:${grpcVersion}")
    compile("io.grpc:grpc-protobuf:${grpcVersion}")
    compile("io.grpc:grpc-stub:${grpcVersion}")
    compile("com.google.protobuf:protobuf-java-util:${protobufVersion}")
    compileOnly("javax.annotation:javax.annotation-api:1.2")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    implementation("com.google.code.gson:gson:2.8.6")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protocVersion}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}
