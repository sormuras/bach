package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModuleLayerBuilderTests {
  @Test
  void ofNotExistingPath() {
    var layer = ModuleLayerBuilder.build(Path.of("does-not-exist"), "module");
    assertTrue(layer.modules().isEmpty());
  }

  @Test
  void ofNotExistingModule() {
    var layer = ModuleLayerBuilder.build("does-not-exist");
    assertTrue(layer.modules().isEmpty());
  }

  @Test
  void ofDotBachBuildModule() {
    var layer = ModuleLayerBuilder.build("build");
    assertTrue(layer.findModule("build").isPresent());
    assertTrue(layer.findModule("com.github.sormuras.bach").isPresent());
  }
}
