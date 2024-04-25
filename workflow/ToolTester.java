/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.module.FindException;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.Tool;
import run.bach.ToolCall;
import run.bach.workflow.Structure.Space;

public interface ToolTester extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();

  static Space space() {
    return Optional.ofNullable(SPACE.get()).orElseThrow(IllegalStateException::new);
  }

  default void testViaTool(Space space) {
    if (SPACE.get() != null) throw new IllegalStateException();
    try {
      SPACE.set(space);
      var names = toolTesterUsesModuleNamesForTesting();
      for (var name : names) {
        var layer = toolTesterUsesModuleLayerToFindModule(name);
        var module = layer.findModule(name).orElseThrow(() -> new FindException(name));
        if (toolTesterDoesHandleModule(module)) {
          say("Testing via running tools provided by module %s ...".formatted(name));
          testViaTool(module);
        }
      }
    } finally {
      SPACE.remove();
    }
  }

  default void testViaTool(Module module) {
    var layer = module.getLayer();
    var providers =
        ServiceLoader.load(layer, ToolProvider.class).stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.name().startsWith("test"))
            .toList();
    for (var provider : providers) {
      var tool = Tool.of(provider);
      var call = ToolCall.of(tool);
      toolTesterRunToolCall(call);
    }
  }

  default List<String> toolTesterUsesModuleNamesForTesting() {
    return space().modules().names();
  }

  default ModuleLayer toolTesterUsesModuleLayerToFindModule(String module) {
    return space().toModuleLayer(workflow().folders(), module);
  }

  default boolean toolTesterDoesHandleModule(Module module) {
    var include =
        module.getDescriptor().provides().stream()
            .anyMatch(provides -> provides.service().equals(ToolProvider.class.getName()));
    if (include) {
      module.getClassLoader().setDefaultAssertionStatus(true);
    }
    return include;
  }

  default void toolTesterRunToolCall(ToolCall call) {
    run(call);
  }
}
