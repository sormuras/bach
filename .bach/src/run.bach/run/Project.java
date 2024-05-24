package run;

import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.util.Optional;
import run.bach.ModuleFinders;
import run.bach.ToolCall;
import run.bach.ToolRunner;
import run.bach.workflow.Builder;
import run.bach.workflow.ClassesCompiler;
import run.bach.workflow.Folders;
import run.bach.workflow.ImageCompiler;
import run.bach.workflow.Starter;
import run.bach.workflow.Structure;
import run.bach.workflow.Structure.Basics;
import run.bach.workflow.Structure.Space;
import run.bach.workflow.Structure.Spaces;
import run.bach.workflow.Workflow;
import run.info.org.junit.JUnit;

public record Project(boolean verbose, Workflow workflow) implements Builder, Starter {
  static Project ofCurrentWorkingDirectory() {
    var verbose = Boolean.getBoolean("-Debug".substring(2));
    var folders = Folders.ofCurrentWorkingDirectory();
    var basics = new Basics("Bach", "2024-ea");
    var main =
        new Space("main")
            .withTargetingJavaRelease(22)
            .withLauncher("bach=run.bach/run.bach.Main")
            .withModule(".bach/src/run.bach", ".bach/src/run.bach/module-info.java")
            .with(Space.Flag.COMPILE_RUNTIME_IMAGE);
    var test =
        new Space("test", main)
            .withLauncher("tests=test.bach/test.bach.Tests")
            .withModule("src/test.bach", "src/test.bach/test/java/module-info.java")
            .withModule("src/test.junit", "src/test.junit/test/java/module-info.java");
    var libraries =
        ModuleFinder.compose(
            ModuleFinders.ofProperties(JUnit.MODULES),
            ModuleFinders.ofProperties(
                "org.junitpioneer=https://repo.maven.apache.org/maven2/org/junit-pioneer/junit-pioneer/2.2.0/junit-pioneer-2.2.0.jar#SIZE=191032"));
    var structure = new Structure(basics, new Spaces(main, test), libraries);
    var runner = ToolRunner.ofSystem();
    return new Project(verbose, new Workflow(folders, structure, runner));
  }

  void printStatus() {
    var structure = workflow.structure();
    System.out.println(structure.toNameAndVersion());
    System.out.println(structure.basics());
    structure.spaces().forEach(System.out::println);
    structure.libraries().findAll().stream().sorted().forEach(System.out::println);
    System.out.println(workflow.folders());
    System.out.println(workflow.runner());
  }

  @Override
  public boolean builderDoesCleanAtTheBeginning() {
    return true;
  }

  @Override
  public void classesCompilerRunJavacToolCall(ToolCall javac) {
    run(javac.add("-X" + "lint:all").add("-W" + "error"));
    // Delete all local programs in out/main/classes/*/run.bach/run/* directory
    if (ClassesCompiler.space().name().equals("main")) {
      var dir = classesCompilerUsesDestinationDirectory().resolve("run.bach", "run");
      try (var stream = Files.newDirectoryStream(dir, Files::isRegularFile)) {
        for (var file : stream) Files.deleteIfExists(file);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  @Override
  public ToolCall modulesCompilerUsesJarToolCall() {
    return Builder.super.modulesCompilerUsesJarToolCall().when(verbose, "--verbose");
  }

  @Override
  public Optional<String> imageCompilerUsesLauncher() {
    if (ImageCompiler.space().name().equals("main")) return Optional.of("bach=run.bach");
    return Optional.empty();
  }

  @Override
  public void junitTesterRunJUnitToolCall(ToolCall junit) {
    run(junit.add("--details", "none").add("--disable-banner").add("--disable-ansi-colors"));
  }
}
