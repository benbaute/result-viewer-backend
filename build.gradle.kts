buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("io.spring.javaformat:spring-javaformat-gradle-plugin:0.0.43")
    }
}

plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("io.spring.javaformat") version "0.0.43"
    id("io.freefair.lombok") version "9.1.0"
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

allprojects {
    group = "com.simra.konsumgandalf"
    version = "0.0.3-SNAPSHOT"

    repositories {
        maven {
            setUrl("https://repo.osgeo.org/repository/release/")
        }
        maven {
            setUrl("https://mvn.slimjars.com")
        }
        mavenLocal()
        mavenCentral()
    }

    apply {
        plugin("io.spring.javaformat")
    }

}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.freefair.lombok")

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-web")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        implementation("org.postgresql:postgresql")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":rides"))
    implementation(project(":osmPlanet"))

    implementation("com.opencsv:opencsv:5.9")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.hibernate.orm:hibernate-spatial:6.6.4.Final")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation("org.hibernate.orm:hibernate-spatial:6.6.2.Final")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}

tasks.getByName<Jar>("jar") {
    enabled = false
}
subprojects {
    if (name in listOf("common", "osmPlanet", "rides", "valhalla")) {
        tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
            enabled = false
        }
    }
}
