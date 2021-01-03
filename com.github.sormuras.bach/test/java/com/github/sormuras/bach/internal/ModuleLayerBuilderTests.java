package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModuleLayerBuilderTests {
  @Test
  void ofNotExistingPath() {
    var layer = new ModuleLayerBuilder(Path.of("does-not-exist"), "module").build();
    assertTrue(layer.modules().isEmpty());
  }

  @Test
  void ofNotExistingModule() {
    var layer = new ModuleLayerBuilder("does-not-exist").build();
    assertTrue(layer.modules().isEmpty());
  }

  @Test
  void ofDotBachBuildModule() {
    var layer = new ModuleLayerBuilder("build").build();
    assertTrue(layer.findModule("build").isPresent());
    assertTrue(layer.findModule("com.github.sormuras.bach").isPresent());
  }
}
