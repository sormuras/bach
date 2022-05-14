package com.github.sormuras.bach.internal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.github.sormuras.bach.CommandLineInterface;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ArgVesterTests {
  static ArgVester<CommandLineInterface> PARSER = ArgVester.create(CommandLineInterface.class);

  @Test
  void emptyArgs() {
    var args = PARSER.parse();
    assertAllOptionalsAndAllRepeatablesAreEmpty(args);
    assertEquals(List.of(), args.command());
  }

  @Test
  void onlyCommand() {
    var args = PARSER.parse("TOOL-NAME", "--foo", "--bar=3");
    assertAllOptionalsAndAllRepeatablesAreEmpty(args);
    assertEquals(List.of("TOOL-NAME", "--foo", "--bar=3"), args.command());
  }

  void assertAllOptionalsAndAllRepeatablesAreEmpty(CommandLineInterface args) {
    assertAll(
        // flags
        () -> assertTrue(args.help().isEmpty()),
        () -> assertTrue(args.verbose().isEmpty()),
        () -> assertTrue(args.dry_run().isEmpty()),
        // paths and path patterns
        () -> assertTrue(args.root_directory().isEmpty()),
        () -> assertTrue(args.output_directory().isEmpty()),
        () -> assertTrue(args.module_info_find_pattern().isEmpty()),
        // project properties
        () -> assertTrue(args.project_name().isEmpty()),
        () -> assertTrue(args.project_version().isEmpty()),
        () -> assertTrue(args.project_version_date().isEmpty()),
        () -> assertTrue(args.project_targets_java().isEmpty()),
        () -> assertTrue(args.project_launcher().isEmpty()),
        () -> assertTrue(args.project_with_external_module().isEmpty()),
        () -> assertTrue(args.project_with_external_modules().isEmpty())
        // inital tool call
        // dont check here () -> assertTrue(args.command().isEmpty())
        );
  }

  @TestFactory
  Stream<DynamicNode> touchAll() {
    var args =
        PARSER.parse(
            // flags
            "--help",
            "--verbose",
            "--dry-run",
            // paths and path patterns
            "--root-directory=ROOT",
            "--output-directory=OUT",
            "--module-info-find-pattern=SYNTAX:PATTERN",
            // project properties
            "--project-name=NAME",
            "--project-version=VERSION",
            "--project-version-date=DATE",
            "--project-targets-java=RELEASE",
            "--project-launcher=LAUNCHER",
            "--project-with-external-module=M@A",
            "--project-with-external-module=N@B",
            "--project-with-external-modules=X@V",
            "--project-with-external-modules=Y:V:C",
            // inital tool call
            "TOOL-NAME",
            "TOOL-ARGS");
    return Stream.of(
        dynamicContainer(
            "flags",
            Stream.of(
                dynamicTest("--help", () -> assertTrue(args.help().orElseThrow())),
                dynamicTest("--verbose", () -> assertTrue(args.verbose().orElseThrow())),
                dynamicTest("--dry-run", () -> assertTrue(args.dry_run().orElseThrow())))),
        dynamicContainer(
            "paths and path patterns",
            Stream.of(
                dynamicCheck("--root-directory", "ROOT", args.root_directory()),
                dynamicCheck("--output-directory", "OUT", args.output_directory()),
                dynamicCheck(
                    "--module-info-find-pattern",
                    "SYNTAX:PATTERN",
                    args.module_info_find_pattern()),
                dynamicCheck(
                    "--project-with-external-module",
                    List.of("M@A", "N@B"),
                    args.project_with_external_module()),
                dynamicCheck(
                    "--project-with-external-modules",
                    List.of("X@V", "Y:V:C"),
                    args.project_with_external_modules()))),
        dynamicContainer(
            "project properties",
            Stream.of(
                dynamicCheck("--project-name", "NAME", args.project_name()),
                dynamicCheck("--project-version", "VERSION", args.project_version()),
                dynamicCheck("--project-version-date", "DATE", args.project_version_date()),
                dynamicCheck("--project-targets-java", "RELEASE", args.project_targets_java()),
                dynamicCheck("--project-launcher", "LAUNCHER", args.project_launcher()))),
        dynamicContainer(
            "initial tool call",
            Stream.of(dynamicCheck("command", List.of("TOOL-NAME", "TOOL-ARGS"), args.command()))));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  <T> DynamicTest dynamicCheck(String displayName, T expected, Optional<T> actual) {
    return dynamicTest(displayName, () -> assertEquals(Optional.of(expected), actual));
  }

  <T> DynamicTest dynamicCheck(String displayName, List<T> expected, List<T> actual) {
    return dynamicTest(displayName, () -> assertEquals(expected, actual));
  }
}
