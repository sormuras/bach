/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.util.function.UnaryOperator;
import run.bach.Bach;
import run.bach.ToolFinder;
import run.bach.ToolRunner;
import run.bach.internal.PathSupport;

public record Workflow(Bach.Folders folders, Structure structure, ToolRunner runner) {
  public static Workflow ofCurrentWorkingDirectory() {
    return of(Bach.Folders.ofCurrentWorkingDirectory());
  }

  public static Workflow of(Bach.Folders folders) {
    var name = System.getProperty("--project-name", PathSupport.name(folders.root(), "Unnamed"));
    var version = System.getProperty("--project-version", "0-ea");
    var basics = new Structure.Basics(name, version);
    var spaces = new Structure.Spaces();
    var structure = new Structure(basics, spaces);
    var runner = ToolRunner.ofSystem();
    return new Workflow(folders, structure, runner);
  }

  public Workflow withName(String name) {
    return new Workflow(folders, structure.withName(name), runner);
  }

  public Workflow withVersion(String name) {
    return new Workflow(folders, structure.withVersion(name), runner);
  }

  public Workflow withTimestamp(String zonedDateTime) {
    return new Workflow(folders, structure.withTimestamp(zonedDateTime), runner);
  }

  public Workflow withMainSpace(UnaryOperator<Structure.Space> operator) {
    return withSpace("main", operator);
  }

  public Workflow withSpace(String name, UnaryOperator<Structure.Space> operator) {
    return with(operator.apply(new Structure.Space(name)));
  }

  public Workflow with(Structure.Space space) {
    return new Workflow(folders, structure.with(space), runner);
  }

  public Workflow with(ToolFinder finder) {
    return with(ToolRunner.of(finder));
  }

  public Workflow with(ToolRunner runner) {
    return new Workflow(folders, structure, runner);
  }
}
