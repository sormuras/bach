package com.github.sormuras.bach;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record Grabber(Bach bach) {

  public Directory newExternalToolLayerDirectory(String name, Asset... assets) {
    return new Directory(bach.path().externalToolLayers(), name, List.of(assets));
  }

  public Directory newExternalToolProgramDirectory(String name, Asset... assets) {
    return new Directory(bach.path().externalToolPrograms(), name, List.of(assets));
  }

  public void grab(Directory... directories) {
    for (var directory : directories) {
      var root = directory.parent().resolve(directory.name());
      for (var asset : directory.assets()) {
        var target = root.resolve(asset.name());
        var arg = target.toString().replace('\\', '/') + '=' + asset.source();
        bach.run("grab", arg);
      }
    }
  }

  public record Directory(Path parent, String name, List<Asset> assets) {
    public Directory withAsset(String target, String source) {
      var assets = new ArrayList<>(assets());
      assets.add(new Asset(target, source));
      return new Directory(parent, name, assets);
    }
  }

  public record Asset(String name, String source) {}
}
