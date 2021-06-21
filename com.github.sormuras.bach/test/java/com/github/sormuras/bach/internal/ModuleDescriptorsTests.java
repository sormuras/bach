package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModuleDescriptorsTests {

  @Test
  void parse() {
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
}
