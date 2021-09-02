package com.github.sormuras.bach.builder;

import com.github.sormuras.bach.Bach;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/** Contains methods creating new builder instances, e.g. building conventional Java projects. */
public record BuilderFactory(Bach bach) {

  public Conventional.Builder conventional() {
    var unnamedSpace =
        new Conventional.Space(
            Optional.empty(), List.of(), List.of("."), List.of(), Path.of("modules"));
    return new Conventional.Builder(bach, unnamedSpace);
  }

  public Conventional.Builder conventional(String space) {
    if (space.isEmpty()) throw new IllegalArgumentException("space name must not be empty");

    var moduleSourcePather = new StringJoiner(File.separator).add(".").add("*").add(space);
    var namedSpace =
        new Conventional.Space(
            Optional.of(space),
            List.of(),
            List.of(moduleSourcePather.toString(), moduleSourcePather.add("java").toString()),
            List.of(),
            Path.of(space, "modules"));
    return new Conventional.Builder(bach, namedSpace);
  }
}
