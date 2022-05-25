package com.github.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** A collection of source and resource directories. */
public record Folders(List<Path> sources, List<Path> resources) {
  public static Folders of(Path... sources) {
    return new Folders(Stream.of(sources).map(Path::normalize).toList(), List.of());
  }

  public Folders withSiblings(Path container) {
    return withSiblings(container, "");
  }

  public Folders withSiblings(Path container, int release) {
    return withSiblings(container, "-" + release);
  }

  public Folders withSiblings(Path container, String suffix) {
    var sources = container.resolve("java" + suffix);
    var resources = container.resolve("resources" + suffix);
    var folders = this;
    if (Files.isDirectory(sources)) folders = folders.withSourcePath(sources);
    if (Files.isDirectory(resources)) folders = folders.withResourcePath(resources);
    return folders;
  }

  public Folders withSourcePath(Path candidate) {
    var path = candidate.normalize();
    if (sources.contains(path)) return this;
    return new Folders(Stream.concat(sources.stream(), Stream.of(path)).toList(), resources);
  }

  public Folders withResourcePath(Path candidate) {
    var path = candidate.normalize();
    if (resources.contains(path)) return this;
    return new Folders(sources, Stream.concat(resources.stream(), Stream.of(path)).toList());
  }

  public boolean isEmpty() {
    return sources.isEmpty() && resources.isEmpty();
  }
}
