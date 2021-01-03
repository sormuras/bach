package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** A builder for building module layers. */
public record ModuleLayerBuilder(Path moduleSourceDirectory, String module) {

  public ModuleLayerBuilder(String module) {
    this(Path.of(".bach"), module);
  }

  public ModuleLayer build() {
    var buildModulePath = moduleSourceDirectory.resolve(module);
    var buildModuleInfo = buildModulePath.resolve("module-info.java");
    if (Files.notExists(buildModuleInfo)) return ModuleLayer.empty();

    var destination = Bach.WORKSPACE.resolve(moduleSourceDirectory);
    compile(destination, Bach.CACHE);
    return layer(destination, Bach.CACHE);
  }

  void compile(Path destination, Path... modulePaths) {
    var modulePath = Stream.of(modulePaths).map(Path::toString).toList();
    var args =
        Command.of("javac")
            .add("--module", module)
            .add("--module-source-path", moduleSourceDirectory)
            .add("--module-path", String.join(File.pathSeparator, modulePath))
            .add("-encoding", "UTF-8")
            .add("-d", destination)
            .toStrings()
            .toArray(String[]::new);
    var result = ToolProvider.findFirst("javac").orElseThrow().run(System.out, System.err, args);
    if (result != 0) throw new RuntimeException("Non-zero exit code: " + result);
  }

  ModuleLayer layer(Path... paths) {
    var before = ModuleFinder.of();
    var finder = ModuleFinder.of(paths);
    var boot = ModuleLayer.boot();
    var configuration = boot.configuration().resolveAndBind(before, finder, Set.of(module));
    var parent = ClassLoader.getPlatformClassLoader();
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(boot), parent);
    return controller.layer();
  }
}
