/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.module.ModuleDescriptor;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.Tool;
import run.bach.ToolCall;
import run.bach.ToolNotFoundException;
import run.bach.workflow.Structure.Space;

public interface JUnitTester extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  default void testViaJUnit(Space space) {
    try {
      SPACE.set(space);
      for (var name : space.modules().names()) {
        var layer = space.toModuleLayer(workflow().folders(), name);
        var module = layer.findModule(name).orElseThrow(AssertionError::new);
        if (junitTesterDoesCheckModule(module)) {
          say("Testing via JUnit scanning module %s for tests ...".formatted(module.getName()));
          testViaJUnit(module);
        }
      }
    } finally {
      SPACE.remove();
    }
  }

  default void testViaJUnit(Module module) {
    var folders = workflow().folders();
    var layer = module.getLayer();
    module.getClassLoader().setDefaultAssertionStatus(true);
    var tool =
        ServiceLoader.load(layer, ToolProvider.class).stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.name().equals("junit"))
            .findFirst()
            .map(Tool::of)
            .orElseThrow(() -> new ToolNotFoundException("junit"));
    var junit =
        ToolCall.of(tool)
            .add("execute")
            .add("--select-module", module.getName())
            .add("--reports-dir", folders.out("test-reports", "junit", module.getName()));
    junitTesterRun(junit);
  }

  default boolean junitTesterDoesCheckModule(Module module) {
    return module.getDescriptor().requires().stream()
        .map(ModuleDescriptor.Requires::name)
        .anyMatch(name -> name.equals("org.junit.platform.console"));
  }

  default void junitTesterRun(ToolCall junit) {
    run(junit);
  }
}
