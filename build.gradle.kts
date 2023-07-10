import com.google.protobuf.gradle.id

plugins {
    `java-library`
    id("com.google.protobuf") version "0.9.3"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.grpc:grpc-stub:1.56.1")
    implementation("io.grpc:grpc-netty:1.56.1")
    implementation("io.grpc:grpc-protobuf:1.56.1")
    implementation("io.grpc:grpc-services:1.56.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.34.4"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.56.1"
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