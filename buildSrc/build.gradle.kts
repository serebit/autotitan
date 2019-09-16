plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("gradle-plugin-api", version = "1.3.50"))
}

kotlin.sourceSets["main"].kotlin.srcDir("src")
