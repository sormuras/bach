package run.bach.tool;

import java.lang.annotation.Annotation;
import java.lang.module.ModuleFinder;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import run.bach.Project;
import run.bach.ProjectOperator;
import run.bach.ProjectRunner;
import run.bach.internal.ModulesSupport;
import run.duke.ToolCall;
import run.duke.ToolLogger;

public class TestTool implements ProjectOperator {
  public static ToolCall test() {
    return ToolCall.of("test");
  }

  public TestTool() {}

  @Override
  public final String name() {
    return "test";
  }

  @Override
  public void run(ProjectRunner runner, ToolLogger logger, String... args) {
    var spaces = runner.project().spaces();
    if (!spaces.names().contains("test")) return;
    var tester = new SpaceTester(runner, spaces.space("test"));
    tester.runSpaceLauncher();
    tester.runJUnitPlatform();
  }

  public static class SpaceTester {
    final ProjectRunner runner;
    final Project.Space space;

    public SpaceTester(ProjectRunner runner, Project.Space space) {
      this.runner = runner;
      this.space = space;
    }

    void runSpaceLauncher() {
      for (var launcher : space.launchers()) runSpaceLauncher(launcher);
    }

    void runSpaceLauncher(String launcher) {
      var folders = runner.folders();
      var java =
          ToolCall.of("java")
              .with("--module-path", space.toRuntimeSpace().toModulePath(folders).orElse("."))
              .with("--module", launcher);
      runner.run(java);
    }

    void runJUnitPlatform() {
      if (runner.findTool("junit").isEmpty()) return;
      for (var module : space.modules()) {
        runJUnitPlatform(module.name());
      }
    }

    void runJUnitPlatform(String name) {
      var folders = runner.folders();
      var finder =
          ModuleFinder.of(
              folders.out("test", "modules", name + ".jar"),
              folders.out("main", "modules"),
              folders.out("test", "modules"),
              folders.externalModules());
      var layer = ModulesSupport.buildModuleLayer(finder, name);
      var module = layer.findModule(name).orElseThrow(AssertionError::new);
      var annotations =
          Stream.of(module.getAnnotations())
              .map(Annotation::annotationType)
              .map(Class::getTypeName)
              .toList();
      if (!annotations.contains("org.junit.platform.commons.annotation.Testable")) return;
      module.getClassLoader().setDefaultAssertionStatus(true);
      var junit =
          ServiceLoader.load(layer, ToolProvider.class).stream()
              .map(ServiceLoader.Provider::get)
              .filter(provider -> provider.name().equals("junit"))
              .findFirst()
              .orElseThrow();
      runner.run(
          ToolCall.of(junit)
              .with("--select-module", name)
              .with("--reports-dir", folders.out("test-reports", "junit-" + name)));
    }
  }
}
