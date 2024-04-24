/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.Tool;
import run.bach.ToolCall;
import run.bach.workflow.Structure.Space;

public interface ToolTester extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  default void testViaTool(Space space) {
    try {
      SPACE.set(space);
      var names = space.modules().names();
      for (var name : names) {
        var layer = space.toModuleLayer(workflow().folders(), name);
        var module = layer.findModule(name).orElseThrow(AssertionError::new);
        if (toolTesterDoesCheckModule(module)) {
          say("Testing via running tools %s provides ...".formatted(name));
          testViaTool(module);
        }
      }
    } finally {
      SPACE.remove();
    }
  }

  default void testViaTool(Module module) {
    var layer = module.getLayer();
    toolTesterDoesEnableAssertions(module);
    var providers =
        ServiceLoader.load(layer, ToolProvider.class).stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.name().startsWith("test"))
            .toList();
    for (var provider : providers) {
      var tool = Tool.of(provider);
      var call = ToolCall.of(tool);
      toolTesterRun(call);
    }
  }

  default boolean toolTesterDoesCheckModule(Module module) {
    return module.getDescriptor().provides().stream()
        .anyMatch(provides -> provides.service().equals(ToolProvider.class.getName()));
  }

  default void toolTesterDoesEnableAssertions(Module module) {
    module.getClassLoader().setDefaultAssertionStatus(true);
  }

  default void toolTesterRun(ToolCall call) {
    run(call);
  }
}
