package com.github.sormuras.bach;

import com.github.sormuras.bach.project.ProjectInfo;
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

  private static final String BUILD = "build";

  private final Bach bach;
  private final ModuleLayer layer;

  BuildModule(Bach bach) {
    this.bach = bach;
    this.layer = computeLayer();
  }

  ModuleLayer computeLayer() {
    var buildModuleCompilationUnit = Path.of(".bach", BUILD, "module-info.java");
    if (Files.notExists(buildModuleCompilationUnit)) {
      bach.debug("No build module (%s) present -> empty layer", buildModuleCompilationUnit);
      return ModuleLayer.empty();
    }

    var cache = Path.of(System.getProperty("bach.cache", ".bach/cache"));
    var classes = Bach.WORKSPACE.resolve(".bach");
    var javac =
        Command.builder("javac")
            .with("--module", BUILD)
            .with("--module-source-path", Path.of(".bach"))
            .with("--module-path", cache)
            .with("-encoding", "UTF-8")
            .with("-d", classes)
            .build();

    bach.debug("Compile build module: %s", javac);
    new ToolRunner().run(javac).checkSuccessful();

    var before = ModuleFinder.of();
    var finder = ModuleFinder.of(classes, cache);
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(before, finder, Set.of(BUILD));
    var parent = ClassLoader.getPlatformClassLoader();
    return ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent).layer();
  }

  Optional<BuildProgram> findBuildProgram() {
    return ServiceLoader.load(layer, BuildProgram.class).findFirst();
  }

  Optional<ProjectInfo> findProjectInfo() {
    return layer.findModule(BUILD).map(module -> module.getAnnotation(ProjectInfo.class));
  }
}