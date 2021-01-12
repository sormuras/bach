package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ModuleLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.StringJoiner;

/** A module lookup that delegates to providers loaded from within the given directory. */
public final class DynamicModuleLookup implements ModuleLookup {

  private final Path directory;
  private ModuleLookup directoryLookup;
  private String directoryNames;

  public DynamicModuleLookup(Path directory) {
    this.directory = directory;
    this.directoryLookup = ModuleLookup.ofEmpty();
    this.directoryNames = "<init>";
  }

  @Override
  public Optional<String> lookupModule(String module) {
    var computedNames = computeNames(directory);
    if (!computedNames.equals(directoryNames)) {
      directoryLookup = computeModuleLookup(directory);
      directoryNames = computedNames;
    }
    return directoryLookup.lookupModule(module);
  }

  @Override
  public String toString() {
    return "DynamicModuleLookup -> " + directoryLookup;
  }

  private static String computeNames(Path directory) {
    if (!Files.isDirectory(directory)) return "";
    var joiner = new StringJoiner("\n");
    try (var stream = Files.newDirectoryStream(directory, "*.jar")) {
      stream.forEach(path -> joiner.add(path.toString()));
      return joiner.toString();
    } catch (Exception exception) {
      throw new RuntimeException("Stream directory failed: " + directory, exception);
    }
  }

  private static ModuleLookup computeModuleLookup(Path directory) {
    if (!Files.isDirectory(directory)) return ModuleLookup.ofEmpty();
    var layer = ModuleLayerBuilder.build(directory);
    var loader = ServiceLoader.load(layer, ModuleLookup.class);
    var lookups = loader.stream().map(ServiceLoader.Provider::get).toList();
    return lookups.isEmpty() ? ModuleLookup.ofEmpty() : new ComposedModuleLookup(lookups);
  }
}
