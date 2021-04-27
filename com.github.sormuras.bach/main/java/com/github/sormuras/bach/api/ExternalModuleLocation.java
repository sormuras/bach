package com.github.sormuras.bach.api;

import com.github.sormuras.bach.api.external.Maven;
import java.util.Optional;

public record ExternalModuleLocation(String module, String uri) implements ExternalModuleLocator {

  public static ExternalModuleLocation of(ProjectInfo.ExternalModule info) {
    var module = info.name();
    var link = info.link();
    var type = type(info);
    if (type == ProjectInfo.LinkType.URI) return new ExternalModuleLocation(module, link);
    if (type == ProjectInfo.LinkType.MAVEN) {
      var repository = info.mavenRepository();
      var split = link.split(":");
      if (split.length < 3) throw new IllegalArgumentException("Not a Maven link: " + info);
      var group = split[0];
      var artifact = split[1];
      var version = split[2];
      var classifier = split.length < 4 ? "" : split[3];
      var joiner = Maven.Joiner.of(group, artifact, version).repository(repository).classifier(classifier);
      return new ExternalModuleLocation(module, joiner.toString());
    }
    throw new IllegalArgumentException("Unsupported link type: " + info);
  }

  private static ProjectInfo.LinkType type(ProjectInfo.ExternalModule info) {
    var type = info.type();
    if (type != ProjectInfo.LinkType.AUTO) return type;
    var link = info.link();
    var maven = link.indexOf('/') == -1 && link.chars().filter(ch -> ch == ':').count() >= 2;
    return maven ? ProjectInfo.LinkType.MAVEN : ProjectInfo.LinkType.URI;
  }

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    return this.module.equals(module) ? Optional.of(this) : Optional.empty();
  }

  @Override
  public Stability stability() {
    return Stability.STABLE;
  }
}
