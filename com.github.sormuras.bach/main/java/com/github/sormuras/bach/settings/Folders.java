package com.github.sormuras.bach.settings;

import java.nio.file.Path;

public record Folders(Path root, Path workspace, Path externalModules, Path externalTools) {

  public static Folders of(String root) {
    return Folders.of(Path.of(root));
  }

  public static Folders of(Path path) {
    var root = path.normalize();
    var workspace = root.resolve(".bach/workspace");
    var externals = root.resolve(".bach/external-modules");
    var tools = root.resolve(".bach/external-tools");
    return new Folders(root, workspace, externals, tools);
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
