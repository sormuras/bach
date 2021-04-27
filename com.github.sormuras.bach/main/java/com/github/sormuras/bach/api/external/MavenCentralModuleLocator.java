package com.github.sormuras.bach.api.external;

import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocations;
import com.github.sormuras.bach.api.ExternalModuleLocator;
import java.util.Optional;

/** Locates modules via their Maven Central artifacts. */
public abstract class MavenCentralModuleLocator implements ExternalModuleLocator {

  private final String group;
  private final String version;
  private final ExternalModuleLocations locations;

  public MavenCentralModuleLocator(String group, String version) {
    this.group = group;
    this.version = version;
    this.locations = newExternalModuleLocations();
  }

  protected abstract ExternalModuleLocations newExternalModuleLocations();

  public String group() {
    return group;
  }

  public String version() {
    return version;
  }

  public ExternalModuleLocation location(String module, String artifact) {
    return new ExternalModuleLocation(module, Maven.central(group(), artifact, version()));
  }

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    return locations.locate(module);
  }
}
