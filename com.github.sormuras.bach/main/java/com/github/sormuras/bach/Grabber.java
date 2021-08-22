package com.github.sormuras.bach;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public record Grabber(Bach bach, ExternalModuleLocators locators) {

  public Directory newExternalToolLayerDirectory(String name, Asset... assets) {
    return new Directory(bach.path().externalToolLayers(), name, List.of(assets));
  }

  public Directory newExternalToolProgramDirectory(String name, Asset... assets) {
    return new Directory(bach.path().externalToolPrograms(), name, List.of(assets));
  }

  public void grab(Path directory, Asset asset) {
    var target = directory.resolve(asset.name());
    var source = asset.source();
    var arg = target.toString().replace('\\', '/') + '=' + source;
    bach.run("grab", grab -> grab.with(arg));
  }

  public void grab(Directory... directories) {
    for (var directory : directories) {
      var root = directory.parent().resolve(directory.name());
      for (var asset : directory.assets()) grab(root, asset);
    }
  }

  public void grabExternalModules(String... modules) {
    var directory = bach.path().externalModules();
    module_loop:
    for (var module : modules) {
      for (var locator : locators.list()) {
        var location = locator.find(module);
        if (location.isEmpty()) continue;
        bach.logMessage(
            Level.DEBUG, "Located module `%s` via %s".formatted(module, locator.caption()));
        grab(directory, new Asset(module + ".jar", location.get()));
        continue module_loop;
      }
      throw new RuntimeException("Can not locate module: " + module);
    }
  }

  public void grabMissingExternalModules() {
    var explorer = bach.explorer();
    var loaded = new TreeSet<String>();
    var difference = new TreeSet<String>();
    while (true) {
      var missing = explorer.listMissingExternalModules();
      if (missing.isEmpty()) break;
      bach.logMessage(
          Level.DEBUG,
          "Grab %d missing external module%s"
              .formatted(missing.size(), missing.size() == 1 ? "" : "s"));
      difference.retainAll(missing);
      if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
      difference.addAll(missing);
      grabExternalModules(missing.toArray(String[]::new));
      loaded.addAll(missing);
    }
    bach.logMessage(
        Level.DEBUG, "Grabbed %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
  }

  public record Asset(String name, String source) {}

  public record Directory(Path parent, String name, List<Asset> assets) {
    public Directory withAsset(String target, String source) {
      var assets = new ArrayList<>(assets());
      assets.add(new Asset(target, source));
      return new Directory(parent, name, assets);
    }
  }
}
