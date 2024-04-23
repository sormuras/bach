package run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
                "bach=run.bach/run.bach.Main",
                new DeclaredModule(
                    Path.of(".bach/src/run.bach"), Path.of(".bach/src/run.bach/module-info.java")))
            .with(Space.Flag.IMAGE);
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
                    Path.of("src/test.junit/test/java/module-info.java"))),
            Set.of());
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
  public Optional<String> imageCompilerUsesLauncher(Space space) {
    if (space.name().equals("main")) return Optional.of("bach=run.bach");
    return Optional.empty();
  }
}
