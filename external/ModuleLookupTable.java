/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.external;

import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import run.bach.internal.PathSupport;

/** Manages zero or more module lookups. */
public record ModuleLookupTable(List<ModuleLookup> lookups) {
  public static ModuleLookupTable of(Path directory) {
    var list =
        PathSupport.list(directory, PathSupport::isPropertiesFile).stream()
            .map(ModuleLookup::ofProperties)
            .toList();
    return new ModuleLookupTable(list);
  }

  public ModuleLookupTable {
    lookups = List.copyOf(lookups);
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    lookups.forEach(lookup -> joiner.add(lookup.description()));
    joiner.add("    %d lookup%s".formatted(lookups.size(), lookups.size() == 1 ? "" : "s"));
    return joiner.toString().indent(indent).stripTrailing();
  }
}
