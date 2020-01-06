plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

version = "0"

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("-module", "de.sormuras.bach.doc.minimal")
        addStringOption("-patch-module", "de.sormuras.bach.doc.minimal=src/main/java")
    }
}
