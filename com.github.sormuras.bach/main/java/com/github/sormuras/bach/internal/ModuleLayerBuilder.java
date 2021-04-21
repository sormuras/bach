package com.github.sormuras.bach.internal;

import java.lang.ModuleLayer.Controller;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ModuleLayerBuilder {

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
