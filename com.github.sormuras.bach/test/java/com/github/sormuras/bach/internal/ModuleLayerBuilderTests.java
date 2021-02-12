package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.Bach;
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
  void ofDefaultInfoModule() {
    var layer = ModuleLayerBuilder.build(Bach.INFO_MODULE);
    assertTrue(layer.findModule(Bach.INFO_MODULE).isPresent());
    assertTrue(layer.findModule("com.github.sormuras.bach").isPresent());
  }
}
