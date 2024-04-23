/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.Tool;
import run.bach.ToolCall;
import run.bach.ToolNotFoundException;
import run.bach.internal.ModulesSupport;
import run.bach.workflow.Structure.Space;

public interface Tester extends Action {
  default void test() {
    say("Testing ...");
    for (var name : testerUsesSpaceNames()) {
      var spaces = workflow().structure().spaces();
      if (!spaces.names().contains(name)) continue;
      var space = spaces.space(name);
      test(space);
    }
  }

  default void test(Space space) {
    testWithSpaceLauncher(space);
    testWithToolProviders(space);
    testWithJUnitPlatform(space);
  }

  default List<String> testerUsesSpaceNames() {
    return List.of("test");
  }

  default boolean testerShouldTestModuleWithJUnitPlatform(Module module) {
    return module.getDescriptor().requires().stream()
        .map(ModuleDescriptor.Requires::name)
        .anyMatch(name -> name.equals("org.junit.platform.console"));
  }

  default boolean testerShouldTestModuleWithToolProviders(Module module) {
    var name = ToolProvider.class.getName();
    return module.getDescriptor().provides().stream()
        .anyMatch(provides -> provides.service().equals(name));
  }

  default ModuleLayer testerUsesModuleLayerForModuleName(Space space, String name) {
    var folders = workflow().folders();
    var finder =
        ModuleFinder.compose(
            ModuleFinder.of(folders.out(space.name(), "modules", name + ".jar")),
            ModuleFinder.of(
                space.requires().stream()
                    .map(required -> folders.out(required, "modules"))
                    .toArray(Path[]::new)),
            ModuleFinder.of(folders.out(space.name(), "modules")),
            ModuleFinder.of(folders.root("lib")));
    return ModulesSupport.buildModuleLayer(finder, name);
  }

  private void testWithSpaceLauncher(Space space) {
    for (var launcher : space.launchers()) testWithSpaceLauncher(space, launcher);
  }

  private void testWithSpaceLauncher(Space space, Structure.Launcher launcher) {
    var folders = workflow().folders();
    var module = launcher.toModuleAndMainClass();
    var java =
        ToolCall.of("java")
            .add("-ea") // enable assertions
            .add("--module-path", space.toRuntimeSpace().toModulePath(folders).orElse("."))
            .add("--module", module);
    testerRunSpaceLauncher(java);
  }

  default void testerRunSpaceLauncher(ToolCall java) {
    run(java);
  }

  private void testWithToolProviders(Space space) {
    for (var module : space.modules()) testWithToolProviders(space, module.name());
  }

  private void testWithToolProviders(Space space, String name) {
    var layer = testerUsesModuleLayerForModuleName(space, name);
    var module = layer.findModule(name).orElseThrow(AssertionError::new);
    if (!testerShouldTestModuleWithToolProviders(module)) return;
    module.getClassLoader().setDefaultAssertionStatus(true);
    var providers =
        ServiceLoader.load(layer, ToolProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.name().startsWith("test"))
            .toList();
    testerRunToolProviders(providers);
  }

  default void testerRunToolProviders(List<ToolProvider> providers) {
    for (var provider : providers) testerRunToolProvider(provider);
  }

  default void testerRunToolProvider(ToolProvider provider) {
    run(ToolCall.of(Tool.of(provider)));
  }

  private void testWithJUnitPlatform(Space space) {
    for (var name : space.modules().names()) {
      var layer = testerUsesModuleLayerForModuleName(space, name);
      var module = layer.findModule(name).orElseThrow(AssertionError::new);
      if (testerShouldTestModuleWithJUnitPlatform(module)) {
        testWithJUnitPlatform(module);
      }
    }
  }

  private void testWithJUnitPlatform(Module module) {
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
    testerRunJUnitPlatform(junit);
  }

  default void testerRunJUnitPlatform(ToolCall junit) {
    run(junit);
  }
}
