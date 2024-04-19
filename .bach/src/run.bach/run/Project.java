package run;

import java.nio.file.Path;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolRunner;
import run.bach.workflow.Builder;
import run.bach.workflow.Structure;
import run.bach.workflow.Tester;
import run.bach.workflow.Workflow;

public record Project(Workflow workflow) implements Builder, Tester {
  static Project ofCurrentWorkingDirectory() {
    return new Project(
        new Workflow(
            Bach.Folders.ofCurrentWorkingDirectory(),
            new Structure(
                new Structure.Basics("Bach", "2024-ea"),
                new Structure.Spaces(
                    new Structure.Space(
                        "main",
                        22,
                        "run.bach/run.bach.Main",
                        new Structure.DeclaredModule(
                            Path.of(".bach/src/run.bach"),
                            Path.of(".bach/src/run.bach/module-info.java"))), // Space "main
                    new Structure.Space(
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
}
