package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.project.ModuleLookup;
import java.util.Map;
import java.util.Optional;

/** Search for a modular JAR file published on Maven Central. */
public class MavenCentralModuleLookup implements ModuleLookup {

  private final SormurasModulesProperties properties;

  public MavenCentralModuleLookup(Bach bach) {
    this.properties = new SormurasModulesProperties(bach, Map.of());
  }

  @Override
  public Optional<String> lookup(String module) {
    return properties.search(module);
  }
}
