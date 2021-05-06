package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.spi.ToolProvider;

public record BachInfoModuleBuilder(Logbook logbook, Options options) {

  static Path location() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    var resolved = module.getLayer().configuration().findModule(module.getName()).orElseThrow();
    var uri = resolved.reference().location().orElseThrow();
    return Path.of(uri);
  }

  public Module build() {
    var root = options.chrootOrDefault();
    var infoFolder = root.resolve(".bach").normalize();
    var infoModule = options.bachInfoOrDefault();
    var layer = newModuleLayer(infoModule, infoFolder);
    return layer.findModule(infoModule).orElse(Bach.class.getModule());
  }

  ModuleLayer newModuleLayer(String module, Path source) {
    var moduleInfo = source.resolve(module).resolve("module-info.java");
    if (Files.notExists(moduleInfo)) return ModuleLayer.empty();
    var directory = String.format("bach-%s-%08x", module, new Random().nextInt());
    var destination = Path.of(System.getProperty("java.io.tmpdir"), directory);
    var args =
        List.of(
            "--module",
            module,
            "--module-source-path",
            source.toString(),
            "--module-path",
            location().toString(),
            "-encoding",
            "UTF-8",
            "-d",
            destination.toString());
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    var out = logbook.printer().out();
    var err = logbook.printer().err();
    var result = javac.run(out, err, args.toArray(String[]::new));
    if (result != 0) throw new RuntimeException("Non-zero exit code: " + result + " with: " + args);
    var boot = ModuleLayer.boot();
    return new ModuleLayerBuilder()
        .bindServices(true)
        .oneLoader(true)
        .parentConfigurations(List.of(boot.configuration()))
        .before(ModuleFinder.of())
        .after(ModuleFinder.of(destination))
        .roots(Set.of(module))
        .parentLayers(List.of(boot))
        .parentLoader(ClassLoader.getPlatformClassLoader())
        .controllerConsumer(controller -> {})
        .build();
  }
}
