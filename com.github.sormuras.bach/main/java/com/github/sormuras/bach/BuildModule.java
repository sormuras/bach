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

  private final String name;
  private final ModuleLayer layer;

  BuildModule() {
    this.name = "build";
    this.layer = computeLayer();
  }

  ModuleLayer computeLayer() {
    if (Files.notExists(Path.of(".bach", name, "module-info.java"))) return ModuleLayer.empty();
    var cache = Path.of(".bach","cache");
    var classes = Project.WORKSPACE.resolve(".bach");
    var compile =
        Command.builder("javac")
            .with("--module", name)
            .with("--module-source-path", Path.of(".bach"))
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
