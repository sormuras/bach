package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolRunner;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/** Provides insights of {@code .bach/build/module-info.java} and friends. */
class BuildModule {

  static BuildModule of(Path directory) {
    return new BuildModule(directory, "build");
  }

  private final Path directory;
  private final String name;
  private final ModuleLayer layer;

  BuildModule(Path directory, String name) {
    this.directory = directory;
    this.name = name;
    this.layer = computeLayer();
  }

  Path path(String first, String... more) {
    return directory.resolve(Path.of(first, more));
  }

  ModuleLayer computeLayer() {
    if (Files.notExists(path(".bach", name, "module-info.java"))) return ModuleLayer.empty();
    var cache = path(".bach","cache");
    var classes = path(".bach","workspace", ".bach");
    var compile =
        Command.builder("javac")
            .with("--module", name)
            .with("--module-source-path", path(".bach"))
            .with("--module-path", cache)
            .with("-encoding", "UTF-8")
            .with("-d", classes)
            .build();

    new ToolRunner().run(compile).checkSuccessful();
    var finder = ModuleFinder.of(classes, cache);
    var layer = Modules.layer(finder, name);
    assert layer.findModule(name).isPresent() : "Module " + name + " not found?!";

    return layer;
  }

  public Optional<BuildProgram> findBuildProgram() {
    return ServiceLoader.load(layer, BuildProgram.class).findFirst();
  }

  public Optional<ProjectInfo> findProjectInfo() {
    return layer.findModule(name).map(module -> module.getAnnotation(ProjectInfo.class));
  }
}
