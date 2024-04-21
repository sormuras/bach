/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.annotation.Annotation;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.bach.Tool;
import run.bach.ToolCall;
import run.bach.internal.ModulesSupport;
import run.bach.workflow.Structure.Space;

public interface Tester extends Action {
  default void test() {
    say("Testing...");
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
    var annotations =
        Stream.of(module.getAnnotations())
            .map(Annotation::annotationType)
            .map(Class::getTypeName)
            .toList();
    return annotations.contains("org.junit.platform.commons.annotation.Testable");
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

  private void testWithSpaceLauncher(Space space, String launcher) {
    var folders = workflow().folders();
    var java =
        ToolCall.of("java")
            .add("-ea") // enable assertions
            .add("--module-path", space.toRuntimeSpace().toModulePath(folders).orElse("."))
            .add("--module", launcher);
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
    providers.forEach(this::testerRunToolProvider);
  }

  default void testerRunToolProvider(ToolProvider provider) {
    run(ToolCall.of(Tool.of(provider)));
  }

  private void testWithJUnitPlatform(Space space) {
    if (workflow().runner().findTool("junit").isEmpty()) return;
    for (var module : space.modules()) testWithJUnitPlatform(space, module.name());
  }

  private void testWithJUnitPlatform(Space space, String name) {
    var folders = workflow().folders();
    var layer = testerUsesModuleLayerForModuleName(space, name);
    var module = layer.findModule(name).orElseThrow(AssertionError::new);
    if (!testerShouldTestModuleWithJUnitPlatform(module)) return;
    module.getClassLoader().setDefaultAssertionStatus(true);
    var junit =
        Tool.of(
            ServiceLoader.load(layer, ToolProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.name().equals("junit"))
                .findFirst()
                .orElseThrow());
    var call =
        ToolCall.of(junit)
            .add("--select-module", name)
            .add("--reports-dir", folders.out("test-reports", "junit-" + name));
    testerRunJUnitPlatform(call);
  }

  default void testerRunJUnitPlatform(ToolCall junit) {
    run(junit);
  }
}
