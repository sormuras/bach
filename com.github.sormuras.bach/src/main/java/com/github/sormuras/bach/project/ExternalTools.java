package com.github.sormuras.bach.project;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/** A sequence of external tool instances. */
public record ExternalTools(List<ExternalTool> list) implements Iterable<ExternalTool> {

  public static ExternalTools of(ExternalTool... tools) {
    return new ExternalTools(List.of(tools));
  }

  @Override
  public Iterator<ExternalTool> iterator() {
    return list.iterator();
  }

  public Stream<ExternalTool> stream() {
    return list.stream();
  }

  public ExternalTools with(ExternalTool... tools) {
    var stream = Stream.concat(list.stream(), Stream.of(tools));
    return new ExternalTools(stream.toList());
  }
}
