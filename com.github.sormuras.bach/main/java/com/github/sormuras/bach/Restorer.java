package com.github.sormuras.bach;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Restorer {

  private final Bach bach;

  public Restorer(Bach bach) {
    this.bach = bach;
  }

  public Restorable describeExternalToolLayer(String name, Asset... assets) {
    return new Restorable(bach.path().externalToolLayers(), name, List.of(assets));
  }

  public Restorable describeExternalToolProgram(String name, Asset... assets) {
    return new Restorable(bach.path().externalToolPrograms(), name, List.of(assets));
  }

  public void restore(Restorable... restorables) {
    for (var restorable : restorables) {
      var directory = restorable.parent().resolve(restorable.name());
      for (var asset : restorable.assets()) {
        var target = directory.resolve(asset.name());
        var arg = target.toString().replace('\\', '/') + '=' + asset.source();
        bach.run("restore", arg);
      }
    }
  }

  public record Restorable(Path parent, String name, List<Asset> assets) {
    public Restorable withAsset(String target, String source) {
      var assets = new ArrayList<>(assets());
      assets.add(new Asset(target, source));
      return new Restorable(parent, name, assets);
    }
  }

  public record Asset(String name, String source) {}
}
