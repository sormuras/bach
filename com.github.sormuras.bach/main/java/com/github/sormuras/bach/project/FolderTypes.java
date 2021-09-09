package com.github.sormuras.bach.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** A collection of folder types. */
public record FolderTypes(Collection<FolderType> values) {

  public static FolderTypes of(FolderType... values) {
    return new FolderTypes(Stream.of(values).sorted().toList());
  }

  public FolderTypes add(FolderType type) {
    if (values.contains(type)) return this;
    var values = new ArrayList<>(this.values);
    values.add(type);
    Collections.sort(values);
    return new FolderTypes(List.copyOf(values));
  }
}
