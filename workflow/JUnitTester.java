/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.Tool;
import run.bach.ToolCall;
import run.bach.ToolNotFoundException;
import run.bach.workflow.Structure.Space;

public interface JUnitTester extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  private Space space() {
    return SPACE.get();
  }

  default void testViaJUnit(Space space) {
    if (SPACE.get() != null) throw new IllegalStateException();
    try {
      SPACE.set(space);
      for (var name : junitTesterUsesModuleNamesForTesting()) {
        var layer = junitTesterUsesModuleLayerToFindModule(name);
        var module = layer.findModule(name).orElseThrow(() -> new FindException(name));
        if (junitTesterDoesHandleModule(module)) {
          say("Testing via JUnit by selecting tests in module %s ...".formatted(name));
          testViaJUnit(module);
        }
      }
    } finally {
      SPACE.remove();
    }
  }

  default void testViaJUnit(Module module) {
    var layer = module.getLayer();
    var tool =
        ServiceLoader.load(layer, ToolProvider.class).stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.name().equals("junit"))
            .findFirst()
            .map(Tool::of)
            .orElseThrow(() -> new ToolNotFoundException("junit"));
    var junit = junitTestUsesJUnitToolCall(tool);
    junit = junitTesterUsesSelector(junit, module);
    junit = junitTesterUsesReportsDirectory(junit, module);
    junitTesterRunJUnitToolCall(junit);
  }

  default List<String> junitTesterUsesModuleNamesForTesting() {
    return space().modules().names();
  }

  default ModuleLayer junitTesterUsesModuleLayerToFindModule(String module) {
    return space().toModuleLayer(workflow().folders(), module);
  }

  default boolean junitTesterDoesHandleModule(Module module) {
    var include =
        module.getDescriptor().requires().stream()
            .map(ModuleDescriptor.Requires::name)
            .anyMatch(name -> name.equals("org.junit.platform.console"));
    if (include) {
      module.getClassLoader().setDefaultAssertionStatus(true);
    }
    return include;
  }

  default ToolCall junitTestUsesJUnitToolCall(Tool tool) {
    return ToolCall.of(tool).add("execute");
  }

  default ToolCall junitTesterUsesSelector(ToolCall junit, Module module) {
    return junit.add("--select-module", module.getName());
  }

  default ToolCall junitTesterUsesReportsDirectory(ToolCall junit, Module module) {
    var folders = workflow().folders();
    var reports = folders.out("test", "reports", "junit", module.getName());
    return junit.add("--reports-dir", reports);
  }

  default void junitTesterRunJUnitToolCall(ToolCall junit) {
    run(junit);
  }
}
