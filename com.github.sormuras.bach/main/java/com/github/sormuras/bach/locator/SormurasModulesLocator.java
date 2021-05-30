package com.github.sormuras.bach.locator;

import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.api.BachException;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import com.github.sormuras.bach.internal.Strings;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class SormurasModulesLocator implements ExternalModuleLocator {

  private final Core core;
  private final String version;
  private /*-*/ Map<String, String> uris;

  public SormurasModulesLocator(Core core, String version) {
    this.version = version;
    this.core = core;
    this.uris = null;
  }

  @Override
  public Stability stability() {
    return Stability.STABLE;
  }

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    if (uris == null)
      try {
        uris = loadUris();
      } catch (Exception exception) {
        throw new BachException(exception);
      }
    var uri = uris.get(module);
    if (uri == null) return Optional.empty();
    return Optional.of(new ExternalModuleLocation(module, uri));
  }

  private synchronized Map<String, String> loadUris() throws Exception {
    var dir = core.folders().tools("sormuras-modules", version);
    var name = "com.github.sormuras.modules@" + version + ".jar";
    var file = dir.resolve(name);
    if (!Files.exists(file)) {
      var uri = "https://github.com/sormuras/modules/releases/download/" + version + "/" + name;
      Files.createDirectories(dir);
      core.httpLoad(uri, file);
    }
    var jar = FileSystems.newFileSystem(file);
    var lines = Strings.lines(jar.getPath("com/github/sormuras/modules/modules.properties"));
    var tree = new TreeMap<String, String>();
    for (var line : lines) {
      var split = line.indexOf('=');
      var module = line.substring(0, split);
      var uri = line.substring(split + 1);
      tree.put(module, uri);
    }
    return tree;
  }

  @Override
  public String title() {
    var modules = uris == null ? "<not loaded>" : uris.size() + " modules";
    return String.format("sormuras/modules@%s -> %s", version, modules);
  }
}
