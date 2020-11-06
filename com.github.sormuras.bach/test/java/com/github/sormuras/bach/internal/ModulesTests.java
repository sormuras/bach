package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModulesTests {
  @Test
  void parseProjectBach() {
     var mains = Modules.parse(Path.of("."), List.of("./*/main/java"));
     assertEquals(Set.of("com.github.sormuras.bach"), mains.keySet());

    var tests = Modules.parse(Path.of("."), List.of("./*/test/java", "./*/test/java-module"));
    assertEquals(Set.of("com.github.sormuras.bach", "test.base", "test.modules"), tests.keySet());
    var bach = tests.get("com.github.sormuras.bach");
    assertLinesMatch(
        """
        java.base
        java.net.http
        jdk.compiler
        jdk.jartool
        jdk.javadoc
        jdk.jdeps
        jdk.jlink
        org.junit.jupiter
        test.base
        """.lines(),
        bach.requires().stream().map(ModuleDescriptor.Requires::name).sorted());

    var preview = Modules.parse(Path.of(""), List.of("./*/test-preview/java"));
    assertSame(Map.of(), preview); // equals would be okay, too
  }
}
