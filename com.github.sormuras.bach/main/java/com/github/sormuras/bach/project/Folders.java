package com.github.sormuras.bach.project;

import java.nio.file.Path;

/** Defines project's directories. */
public record Folders(Path root, Path externalModules, Path externalTools, Path workspace) {

  public static Folders of(String root, String... more) {
    return of(Path.of(root, more));
  }

  public static Folders of(Path root) {
    var externalModules = root.resolve(".bach/external-modules");
    var externalTools = root.resolve(".bach/external-tools");
    var workspace = root.resolve(".bach/workspace");
    return new Folders(root, externalModules, externalTools, workspace);
  }

  public Path root(String first, String... more) {
    return root.resolve(Path.of(first, more));
  }

  public Path externalModules(String first, String... more) {
    return externalModules.resolve(Path.of(first, more));
  }

  public Path externalTools(String first, String... more) {
    return externalTools.resolve(Path.of(first, more));
  }

  public Path workspace(String first, String... more) {
    return workspace.resolve(Path.of(first, more));
  }
}
