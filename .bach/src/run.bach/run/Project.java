package run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolRunner;
import run.bach.workflow.Builder;
import run.bach.workflow.Launcher;
import run.bach.workflow.Structure;
import run.bach.workflow.Structure.Basics;
import run.bach.workflow.Structure.DeclaredModule;
import run.bach.workflow.Structure.DeclaredModules;
import run.bach.workflow.Structure.Space;
import run.bach.workflow.Structure.Spaces;
import run.bach.workflow.Workflow;

public record Project(boolean verbose, Workflow workflow) implements Builder, Launcher {
  static Project ofCurrentWorkingDirectory() {
    var verbose = Boolean.getBoolean("-Debug".substring(2));
    var folders = Bach.Folders.ofCurrentWorkingDirectory();
    var basics = new Basics("Bach", "2024-ea");
    var main =
        new Space(
            "main",
            22,
            "run.bach/run.bach.Main",
            new DeclaredModule(
                Path.of(".bach/src/run.bach"), Path.of(".bach/src/run.bach/module-info.java")));
    var test =
        new Space(
            "test",
            List.of("main"),
            0,
            List.of(),
            new DeclaredModules(
                new DeclaredModule(
                    Path.of("src/test.bach"), Path.of("src/test.bach/test/java/module-info.java")),
                new DeclaredModule(
                    Path.of("src/test.junit"),
                    Path.of("src/test.junit/test/java/module-info.java"))));
    var structure = new Structure(basics, new Spaces(main, test));
    var runner = ToolRunner.ofSystem();
    return new Project(verbose, new Workflow(folders, structure, runner));
  }

  void printStatus() {
    System.out.println(workflow.structure().toNameAndVersion());
    System.out.println(workflow.structure().basics());
    System.out.println(workflow.structure().modules());
    System.out.println(workflow.folders());
    System.out.println(workflow.runner());
  }

  @Override
  public boolean builderShouldInvokeCleanBeforeCompile() {
    return true;
  }

  @Override
  public void compileClasses(Space space) {
    Builder.super.compileClasses(space);
    // Delete all local programs in out/main/classes/*/run.bach/run/* directory
    if (space.name().equals("main")) {
      var dir = classesCompilerUsesDestinationDirectory(space).resolve("run.bach", "run");
      try (var stream = Files.newDirectoryStream(dir, Files::isRegularFile)) {
        for (var file : stream) Files.deleteIfExists(file);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  @Override
  public void classesCompilerRunJavacToolCall(ToolCall javac) {
    run(javac.add("-X" + "lint:all").add("-W" + "error"));
  }

  @Override
  public ToolCall modulesCompilerNewJarToolCall() {
    return Builder.super.modulesCompilerNewJarToolCall().when(verbose, "--verbose");
  }

  @Override
  public String restorerUsesUriForModuleName(String name) {
    return switch (name) {
        // JUnit
      case "org.apiguardian.api" ->
          "https://repo.maven.apache.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar#SIZE=6806";
      case "org.junit.jupiter" ->
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter/5.10.2/junit-jupiter-5.10.2.jar#SIZE=6359";
      case "org.junit.jupiter.api" ->
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar#SIZE=210956";
      case "org.junit.jupiter.engine" ->
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.2/junit-jupiter-engine-5.10.2.jar#SIZE=244690";
      case "org.junit.jupiter.migrationsupport" ->
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-migrationsupport/5.10.2/junit-jupiter-migrationsupport-5.10.2.jar#SIZE=27713";
      case "org.junit.jupiter.params" ->
          "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-params/5.10.2/junit-jupiter-params-5.10.2.jar#SIZE=586027";
      case "org.junit.platform.commons" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-commons/1.10.2/junit-platform-commons-1.10.2.jar#SIZE=106232";
      case "org.junit.platform.console" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console/1.10.2/junit-platform-console-1.10.2.jar#SIZE=545571";
      case "org.junit.platform.engine" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-engine/1.10.2/junit-platform-engine-1.10.2.jar#SIZE=204821";
      case "org.junit.platform.jfr" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-jfr/1.10.2/junit-platform-jfr-1.10.2.jar#SIZE=19138";
      case "org.junit.platform.launcher" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.10.2/junit-platform-launcher-1.10.2.jar#SIZE=183814";
      case "org.junit.platform.reporting" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-reporting/1.10.2/junit-platform-reporting-1.10.2.jar#SIZE=106950";
      case "org.junit.platform.suite" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-suite/1.10.2/junit-platform-suite-1.10.2.jar#SIZE=6362";
      case "org.junit.platform.suite.api" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-suite-api/1.10.2/junit-platform-suite-api-1.10.2.jar#SIZE=22575";
      case "org.junit.platform.suite.commons" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-suite-commons/1.10.2/junit-platform-suite-commons-1.10.2.jar#SIZE=17326";
      case "org.junit.platform.suite.engine" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-suite-engine/1.10.2/junit-platform-suite-engine-1.10.2.jar#SIZE=24117";
      case "org.junit.platform.testkit" ->
          "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-testkit/1.10.2/junit-platform-testkit-1.10.2.jar#SIZE=44401";
      case "org.junit.vintage.engine" ->
          "https://repo.maven.apache.org/maven2/org/junit/vintage/junit-vintage-engine/5.10.2/junit-vintage-engine-5.10.2.jar#SIZE=67479";
      case "org.opentest4j" ->
          "https://repo.maven.apache.org/maven2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar#SIZE=14304";
        // JUnit Pioneer
      case "org.junitpioneer" ->
          "https://repo.maven.apache.org/maven2/org/junit-pioneer/junit-pioneer/2.2.0/junit-pioneer-2.2.0.jar#SIZE=191032";
      default -> Builder.super.restorerUsesUriForModuleName(name);
    };
  }
}
