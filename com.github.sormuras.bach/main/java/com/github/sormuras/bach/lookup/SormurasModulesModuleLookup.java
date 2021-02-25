package com.github.sormuras.bach.lookup;

import com.github.sormuras.bach.Bach;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Look up unique modules published at Maven Central.
 *
 * @see <a href="https://github.com/sormuras/modules">sormuras/modules</a>
 */
public class SormurasModulesModuleLookup implements ModuleLookup {

  private final Bach bach;
  private final String version;
  private /*-*/ Map<String, String> uris;

  public SormurasModulesModuleLookup(Bach bach, String version) {
    this.bach = bach;
    this.version = version;
    this.uris = null;
  }

  @Override
  public LookupStability lookupStability() {
    return LookupStability.STABLE;
  }

  @Override
  public Optional<String> lookupUri(String module) {
    if (uris == null)
      try {
        uris = loadUris();
      } catch (Exception exception) {
        throw new RuntimeException("Load URIs failed: " + exception.getMessage(), exception);
      }
    return Optional.ofNullable(uris.get(module));
  }

  private synchronized Map<String, String> loadUris() throws Exception {
    var dir = bach.folders().externalTools("sormuras-modules", version);
    var name = "com.github.sormuras.modules@" + version + ".jar";
    var file = dir.resolve(name);
    if (!Files.exists(file)) {
      var uri = "https://github.com/sormuras/modules/releases/download/" + version + "/" + name;
      Files.createDirectories(dir);
      bach.browser().load(uri, file);
    }
    var jar = FileSystems.newFileSystem(file);
    var lines = Files.readAllLines(jar.getPath("com/github/sormuras/modules/modules.properties"));
    var tree = new TreeMap<String, String>();
    for (var line : lines) {
      var split = line.indexOf('=');
      var module = ModuleLookup.requireValidModuleName(line.substring(0, split));
      var uri = ModuleLookup.requireValidUri(line.substring(split + 1));
      tree.put(module, uri);
    }
    return tree;
  }

  @Override
  public String toString() {
    var modules = uris == null ? "<not loaded>" : uris.size() + " modules";
    return String.format("sormuras/modules@%s -> %s", version, modules);
  }
}
