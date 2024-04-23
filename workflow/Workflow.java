/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import run.bach.Bach;
import run.bach.ToolRunner;
import run.bach.internal.PathSupport;

public record Workflow(Bach.Folders folders, Structure structure, ToolRunner runner) {
  public static Workflow of(Bach.Folders folders) {
    var name = System.getProperty("--project-name", PathSupport.name(folders.root(), "Unnamed"));
    var version = System.getProperty("--project-version", "0-ea");
    var basics = new Structure.Basics(name, version);
    var spaces = new Structure.Spaces();
    var structure = new Structure(basics, spaces);
    var runner = ToolRunner.ofSystem();
    return new Workflow(folders, structure, runner);
  }

  public Workflow with(Structure.Space space) {
    return new Workflow(folders, structure.with(space), runner);
  }
}
