package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.ProjectInfo;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModuleLayerBuilderTests {
  @Test
  void ofNotExistingPath() {
    var path = Path.of("does-not-exist");
    var layer = ModuleLayerBuilder.build(path, "module", path);
    assertTrue(layer.modules().isEmpty());
  }

  @Test
  void ofNotExistingModule() {
    var layer = ModuleLayerBuilder.build("does-not-exist");
    assertTrue(layer.modules().isEmpty());
  }

  @Test
  void ofDefaultInfoModule() {
    var layer = ModuleLayerBuilder.build(ProjectInfo.MODULE);
    assertTrue(layer.findModule(ProjectInfo.MODULE).isPresent());
    assertTrue(layer.findModule("com.github.sormuras.bach").isPresent());
  }
}
