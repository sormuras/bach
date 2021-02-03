package com.github.sormuras.bach.lookup;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.spi.ToolProvider;

/** Find and run tool providers to lookup modules. */
public class ToolProvidersModuleLookup implements ModuleLookup {

  private final Bach bach;
  private final Path directory;

  public ToolProvidersModuleLookup(Bach bach, Path directory) {
    this.bach = bach;
    this.directory = directory;
  }

  @Override
  public Optional<String> lookupModule(String module) {
    if (!Files.isDirectory(directory)) return Optional.empty();

    var layer = new ModuleLayerBuilder().before(ModuleFinder.of(directory)).build();
    var tools =
        ServiceLoader.load(layer, ToolProvider.class).stream()
            .filter(provider -> provider.type().getName().contains("ModuleLookup"))
            .map(Provider::get)
            .toList();

    for (var tool : tools) {
      bach.debug("Lookup module %s via %s", module, tool.getClass().getName());
      var string = new StringWriter();
      var writer = new PrintWriter(string);
      var result = tool.run(writer, writer, module);
      if (result == 0) return Optional.of(string.toString().trim());
    }

    return Optional.empty();
  }

  @Override
  public String toString() {
    return "ToolProvidersModuleLookup (" + directory + ")";
  }
}
