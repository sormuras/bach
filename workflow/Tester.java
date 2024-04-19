/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.lang.module.ModuleFinder;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.Tool;
import run.bach.ToolCall;
import run.bach.internal.ModulesSupport;
import run.bach.workflow.Structure.Space;

public interface Tester extends Action {
  default void test() {
    var spaces = workflow().structure().spaces();
    if (!spaces.names().contains("test")) return;
    var space = spaces.space("test");
    var tester = new SpaceTester(workflow(), space);
    tester.testWithSpaceLauncher();
    tester.testWithJUnitPlatform();
  }

  class SpaceTester {
    final Workflow workflow;
    final Space space;

    public SpaceTester(Workflow workflow, Space space) {
      this.workflow = workflow;
      this.space = space;
    }

    void testWithSpaceLauncher() {
      for (var launcher : space.launchers()) testWithSpaceLauncher(launcher);
    }

    void testWithSpaceLauncher(String launcher) {
      var folders = workflow.folders();
      var java =
          ToolCall.of("java")
              .add("--module-path", space.toRuntimeSpace().toModulePath(folders).orElse("."))
              .add("--module", launcher);
      workflow.runner().run(java);
    }

    void testWithJUnitPlatform() {
      if (workflow.runner().findTool("junit").isEmpty()) return;
      for (var module : space.modules()) {
        testWithJUnitPlatform(module.name());
      }
    }

    void testWithJUnitPlatform(String name) {
      var folders = workflow.folders();
      var finder =
          ModuleFinder.of(
              folders.out("test", "modules", name + ".jar"),
              folders.out("main", "modules"),
              folders.out("test", "modules"),
              folders.root("lib"));
      var layer = ModulesSupport.buildModuleLayer(finder, name);
      var module = layer.findModule(name).orElseThrow(AssertionError::new);
      /*
      var annotations =
          Stream.of(module.getAnnotations())
              .map(Annotation::annotationType)
              .map(Class::getTypeName)
              .toList();
      if (!annotations.contains("org.junit.platform.commons.annotation.Testable")) return;
      */
      module.getClassLoader().setDefaultAssertionStatus(true);
      var junit =
          ServiceLoader.load(layer, ToolProvider.class).stream()
              .map(ServiceLoader.Provider::get)
              .filter(provider -> provider.name().equals("junit"))
              .findFirst()
              .orElseThrow();
      workflow
          .runner()
          .run(
              Tool.of(junit),
              args ->
                  args.add("--select-module", name)
                      .add("--reports-dir", folders.out("test-reports", "junit-" + name)));
    }
  }
}
