package com.github.sormuras.bach;

import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolRunner;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

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
    var boot = ModuleLayer.boot();
    var before = ModuleFinder.of();
    var configuration = boot.configuration().resolveAndBind(before, finder, Set.of(name));
    var parent = ClassLoader.getPlatformClassLoader();
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
    var layer = controller.layer();
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
