/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.annotation.Annotation;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.bach.Tool;
import run.bach.ToolCall;
import run.bach.internal.ModulesSupport;
import run.bach.workflow.Structure.Space;

public interface Tester extends Action {
  default void test() {
    var name = testerUsesSpaceName();
    var spaces = workflow().structure().spaces();
    if (!spaces.names().contains(name)) return;
    var space = spaces.space(name);
    test(space);
  }

  default void test(Space space) {
    testWithSpaceLauncher(space);
    testWithJUnitPlatform(space);
  }

  default String testerUsesSpaceName() {
    return "test";
  }

  default boolean testerShouldTestModuleWithJUnitPlatform(Module module) {
    var annotations =
        Stream.of(module.getAnnotations())
            .map(Annotation::annotationType)
            .map(Class::getTypeName)
            .toList();
    return annotations.contains("org.junit.platform.commons.annotation.Testable");
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
    workflow().runner().run(java);
  }

  private void testWithJUnitPlatform(Space space) {
    if (workflow().runner().findTool("junit").isEmpty()) return;
    for (var module : space.modules()) {
      testWithJUnitPlatform(space, module.name());
    }
  }

  private void testWithJUnitPlatform(Space space, String name) {
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
    var layer = ModulesSupport.buildModuleLayer(finder, name);
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
    workflow().runner().run(call);
  }
}
