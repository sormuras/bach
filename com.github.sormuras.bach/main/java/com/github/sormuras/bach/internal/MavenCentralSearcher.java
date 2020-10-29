package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.module.ModuleSearcher;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

/** Search for a modular JAR file published on Maven Central. */
public class MavenCentralSearcher implements ModuleSearcher {

  private final SormurasModulesProperties properties;

  public MavenCentralSearcher(Bach bach) {
    this.properties = new SormurasModulesProperties(bach, Map.of());
  }

  @Override
  public Optional<URI> search(String module) {
    return properties.search(module);
  }
}
