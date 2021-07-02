package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModuleDescriptorsTests {

  @Test
  void parseBachsMainModule() {
    var path = Path.of("com.github.sormuras.bach/main/java/module-info.java");
    var module = ModuleDescriptors.parse(path);

    assertEquals(
        ModuleDescriptor.newModule("com.github.sormuras.bach")
            .requires("java.base")
            .requires("java.net.http")
            .requires("jdk.compiler")
            .requires("jdk.crypto.ec")
            .requires("jdk.jartool")
            .requires("jdk.javadoc")
            .requires("jdk.jdeps")
            .requires("jdk.jfr")
            .requires("jdk.jlink")
            .build(),
        module);
  }

  @Test
  void parseBachsTestModule() {
    var path = Path.of("com.github.sormuras.bach/test/java-module/module-info.java");
    var module = ModuleDescriptors.parse(path);

    assertEquals(
        ModuleDescriptor.newModule("com.github.sormuras.bach")
            .requires("java.base")
            .requires("java.net.http")
            .requires("jdk.compiler")
            .requires("jdk.crypto.ec")
            .requires("jdk.jartool")
            .requires("jdk.javadoc")
            .requires("jdk.jdeps")
            .requires("jdk.jfr")
            .requires("jdk.jlink")
            .requires("org.junit.jupiter")
            .requires("test.base")
            .build(),
        module);
  }

  @Test
  void parseTestBaseModule() {
    var path = Path.of("test.base/test/java/module-info.java");
    var module = ModuleDescriptors.parse(path);

    assertEquals(
        ModuleDescriptor.newModule("test.base")
            .requires("java.base")
            .requires("jdk.httpserver")
            .requires("jdk.xml.dom")
            .requires("org.junit.jupiter")
            .build(),
        module);
  }

  @Test
  void parseTestIntegrationModule() {
    var path = Path.of("test.integration/test/java/module-info.java");
    var module = ModuleDescriptors.parse(path);

    assertEquals(
        ModuleDescriptor.newModule("test.integration")
            .requires("com.github.sormuras.bach")
            .requires("java.base")
            .requires("org.junit.jupiter")
            .requires("test.base")
            .build(),
        module);
  }

  @Test
  void parseTestProjectsModule() {
    var path = Path.of("test.projects/test/java/module-info.java");
    var module = ModuleDescriptors.parse(path);

    assertEquals(
        ModuleDescriptor.newModule("test.projects")
            .requires("com.github.sormuras.bach")
            .requires("java.base")
            .requires("org.junit.jupiter")
            .requires("test.base")
            .build(),
        module);
  }
}
