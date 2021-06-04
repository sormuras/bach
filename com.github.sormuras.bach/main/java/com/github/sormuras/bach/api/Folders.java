package com.github.sormuras.bach.api;

import com.github.sormuras.bach.internal.Strings;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;

public record Folders(Path root, Path workspace, Path externals, Path tools) {

  public static Folders of(String root) {
    return Folders.of(Path.of(root));
  }

  public static Folders of(Path path) {
    var root = path.normalize();
    var workspace = root.resolve(ProjectInfo.FOLDER_WORKSPACE);
    var externals = root.resolve(ProjectInfo.FOLDER_EXTERNAL_MODULES);
    var tools = root.resolve(ProjectInfo.FOLDER_EXTERNAL_TOOLS);
    return new Folders(root, workspace, externals, tools);
  }

  public Path root(String first, String... more) {
    return root.resolve(Path.of(first, more));
  }

  public Path externals(String first, String... more) {
    return externals.resolve(Path.of(first, more));
  }

  public Path tools(String first, String... more) {
    return tools.resolve(Path.of(first,more));
  }

  public Path workspace(String first, String... more) {
    return workspace.resolve(Path.of(first, more));
  }

  public Path modules(CodeSpace space) {
    return workspace("modules" + space.suffix());
  }

  public Path modules(CodeSpace space, String first, String... more) {
    return modules(space).resolve(Path.of(first, more));
  }

  public Path jar(CodeSpace space, String module, Version version) {
    var jar = module + '@' + Strings.toNumberAndPreRelease(version) + space.suffix() + ".jar";
    return modules(space, jar);
  }

}
