package com.github.sormuras.bach.api;

import com.github.sormuras.bach.locator.Maven;
import java.util.Optional;
import java.util.function.Supplier;

public record ExternalModuleLocation(String module, String uri) implements ExternalModuleLocator {

  public static ExternalModuleLocation ofCommandLine(Supplier<String> supplier) {
    var module = supplier.get();
    var link = supplier.get();
    var type = detectLinkType(link);
    return ExternalModuleLocation.of(module, link, type, Maven.CENTRAL_REPOSITORY);
  }

  public static ExternalModuleLocation ofInfo(ProjectInfo.ExternalModule info) {
    var module = info.name();
    var link = info.link();
    var type = info.type() == LinkType.AUTO ? detectLinkType(link) : info.type();
    return ExternalModuleLocation.of(module, link, type, info.mavenRepository());
  }

  private static LinkType detectLinkType(String link) {
    var maven = link.indexOf('/') == -1 && link.chars().filter(ch -> ch == ':').count() >= 2;
    return maven ? LinkType.MAVEN : LinkType.URI;
  }

  private static ExternalModuleLocation of(
      String module, String link, LinkType type, String mavenRepository) {
    if (type == LinkType.URI) return new ExternalModuleLocation(module, link);
    if (type == LinkType.MAVEN) {
      var split = link.split(":");
      if (split.length < 3) throw new IllegalArgumentException("Not a Maven link: " + link);
      var group = split[0];
      var artifact = split[1];
      var version = split[2];
      var classifier = split.length < 4 ? "" : split[3];
      var joiner =
          Maven.Joiner.of(group, artifact, version)
              .repository(mavenRepository)
              .classifier(classifier);
      return new ExternalModuleLocation(module, joiner.toString());
    }
    throw new IllegalArgumentException("Unsupported link type: " + type);
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
