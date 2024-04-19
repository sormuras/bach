package run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolRunner;
import run.bach.workflow.Builder;
import run.bach.workflow.Structure;
import run.bach.workflow.Structure.Space;
import run.bach.workflow.Tester;
import run.bach.workflow.Workflow;

public record Project(boolean verbose, Workflow workflow) implements Builder, Tester {
  static Project ofCurrentWorkingDirectory() {
    return new Project(
        Boolean.getBoolean("-Debug".substring(2)),
        new Workflow(
            Bach.Folders.ofCurrentWorkingDirectory(),
            new Structure(
                new Structure.Basics("Bach", "2024-ea"),
                new Structure.Spaces(
                    new Space(
                        "main",
                        22,
                        "run.bach/run.bach.Main",
                        new Structure.DeclaredModule(
                            Path.of(".bach/src/run.bach"),
                            Path.of(".bach/src/run.bach/module-info.java"))), // Space "main
                    new Space(
                        "test",
                        List.of("main"),
                        0,
                        List.of(),
                        new Structure.DeclaredModules(
                            new Structure.DeclaredModule(
                                Path.of("src/test.bach"),
                                Path.of(
                                    "src/test.bach/test/java/module-info.java")))) // Space "test"
                    ) // Spaces
                ),
            ToolRunner.ofSystem()) // Workflow
        ); // Project
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
  public ToolCall compileClassesWithFinalTouch(ToolCall javac, Space space) {
    return javac.add("-X" + "lint:all").add("-W" + "error");
  }

  @Override
  public ToolCall packageClassesCreateJarCall() {
    return Builder.super.packageClassesCreateJarCall().when(verbose, "--verbose");
  }
}
