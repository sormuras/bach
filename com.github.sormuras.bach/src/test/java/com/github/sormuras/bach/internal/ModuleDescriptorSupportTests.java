package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModuleDescriptorSupportTests {

  @Test
  void parseBachsMainModule() {
    var path = Path.of("com.github.sormuras.bach/src/main/java/module-info.java");
    var module = ModuleDescriptorSupport.parse(path);

    assertEquals(
        ModuleDescriptor.newModule("com.github.sormuras.bach")
            .requires("java.base")
            .requires("jdk.compiler")
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
    var path = Path.of("com.github.sormuras.bach/src/test/java-module/module-info.java");
    var module = ModuleDescriptorSupport.parse(path);

    assertEquals(
        ModuleDescriptor.newOpenModule("com.github.sormuras.bach")
            .requires("java.base")
            .requires("jdk.compiler")
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
    var path = Path.of("test.base/src/test/java/module-info.java");
    var module = ModuleDescriptorSupport.parse(path);

    assertEquals(
        ModuleDescriptor.newOpenModule("test.base")
            .requires("java.base")
            .requires("jdk.httpserver")
            .requires("jdk.xml.dom")
            .requires("org.junit.jupiter")
            .build(),
        module);
  }

  @Test
  void parseTestIntegrationModule() {
    var path = Path.of("test.integration/src/test/java/module-info.java");
    var module = ModuleDescriptorSupport.parse(path);

    assertEquals(
        ModuleDescriptor.newOpenModule("test.integration")
            .requires("com.github.sormuras.bach")
            .requires("java.base")
            .requires("org.junit.jupiter")
            .requires("test.base")
            .build(),
        module);
  }
}
