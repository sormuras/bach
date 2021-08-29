package com.github.sormuras.bach.builder;

import com.github.sormuras.bach.Bach;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/** Contains methods creating new builder instances, e.g. building conventional Java projects. */
public record BuilderFactory(Bach bach) {

  public Conventional.Builder conventional(String... modules) {
    if (modules.length == 0) throw new IllegalArgumentException("modules array must not be empty");

    var unnamedSpace =
        new Conventional.Space(
            Optional.empty(), List.of(modules), List.of("."), List.of(), Path.of("modules"));
    return new Conventional.Builder(bach, unnamedSpace);
  }

  public Conventional.Builder conventionalSpace(String name, String... modules) {
    if (modules.length == 0) throw new IllegalArgumentException("modules array must not be empty");
    if (name.isEmpty()) throw new IllegalArgumentException("name must not be empty");

    var moduleSourcePather = new StringJoiner(File.separator).add(".").add("*").add(name);
    var namedSpace =
        new Conventional.Space(
            Optional.of(name),
            List.of(modules),
            List.of(moduleSourcePather.toString(), moduleSourcePather.add("java").toString()),
            List.of(),
            Path.of(name, "modules"));
    return new Conventional.Builder(bach, namedSpace);
  }
}
