package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.util.Paths;
import java.lang.ModuleLayer.Controller;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

/** A builder for building module layers. */
public class ModuleLayerBuilder {

  public static ModuleLayer build(String module) {
    var bach = Path.of(".bach");
    return build(bach, module, bach.resolve("bin"));
  }

  public static ModuleLayer build(Path dotBach, String module, Path bin) {
    var modulePath = dotBach.resolve(module);
    var moduleInfo = modulePath.resolve("module-info.java");
    if (Files.notExists(moduleInfo)) return ModuleLayer.empty();

    var destination = Paths.createTempDirectory("bach-");
    var args =
        Command.of("javac")
            .add("--module", module)
            .add("--module-source-path", dotBach)
            .add("--module-path", bin)
            .add("-encoding", "UTF-8")
            .add("-d", destination)
            .arguments()
            .toArray(String[]::new);
    var result = ToolProvider.findFirst("javac").orElseThrow().run(System.out, System.err, args);
    if (result != 0) throw new RuntimeException("Non-zero exit code: " + result);

    var boot = ModuleLayer.boot();
    return new ModuleLayerBuilder()
        .bindServices(true)
        .oneLoader(true)
        .parentConfigurations(List.of(boot.configuration()))
        .before(ModuleFinder.of())
        .after(ModuleFinder.of(destination.resolve(module), bin))
        .roots(Set.of(module))
        .parentLayers(List.of(boot))
        .parentLoader(ClassLoader.getPlatformClassLoader())
        .controllerConsumer(controller -> {})
        .build();
  }

  private boolean bindServices = true;
  private boolean oneLoader = true;
  private List<Configuration> parentConfigurations = List.of(ModuleLayer.boot().configuration());
  private ModuleFinder before = ModuleFinder.of();
  private ModuleFinder after = ModuleFinder.of();
  private Collection<String> roots = Set.of();
  private List<ModuleLayer> parentLayers = List.of(ModuleLayer.boot());
  private ClassLoader parentLoader = ClassLoader.getPlatformClassLoader();
  private Consumer<Controller> controllerConsumer = controller -> {};

  public ModuleLayerBuilder() {}

  public ModuleLayerBuilder bindServices(boolean bindServices) {
    this.bindServices = bindServices;
    return this;
  }

  public ModuleLayerBuilder oneLoader(boolean oneLoader) {
    this.oneLoader = oneLoader;
    return this;
  }

  public ModuleLayerBuilder parentConfigurations(List<Configuration> parentConfigurations) {
    this.parentConfigurations = parentConfigurations;
    return this;
  }

  public ModuleLayerBuilder before(ModuleFinder before) {
    this.before = before;
    return this;
  }

  public ModuleLayerBuilder after(ModuleFinder after) {
    this.after = after;
    return this;
  }

  public ModuleLayerBuilder roots(Collection<String> roots) {
    this.roots = roots;
    return this;
  }

  public ModuleLayerBuilder parentLayers(List<ModuleLayer> parentLayers) {
    this.parentLayers = parentLayers;
    return this;
  }

  public ModuleLayerBuilder parentLoader(ClassLoader parentLoader) {
    this.parentLoader = parentLoader;
    return this;
  }

  public ModuleLayerBuilder controllerConsumer(Consumer<Controller> controllerConsumer) {
    this.controllerConsumer = controllerConsumer;
    return this;
  }

  public ModuleLayer build() {
    var configuration =
        bindServices
            ? Configuration.resolveAndBind(before, parentConfigurations, after, roots)
            : Configuration.resolve(before, parentConfigurations, after, roots);
    var controller =
        oneLoader
            ? ModuleLayer.defineModulesWithOneLoader(configuration, parentLayers, parentLoader)
            : ModuleLayer.defineModulesWithManyLoaders(configuration, parentLayers, parentLoader);
    controllerConsumer.accept(controller);
    return controller.layer();
  }
}
