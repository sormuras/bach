package com.github.sormuras.bach.project;

import java.nio.file.Path;
import java.util.List;

/** Test code space. */
public record TestSpace(List<String> modules, List<Path> modulePaths, Tweaks tweaks)
    implements Space {
  @Override
  public String name() {
    return "test";
  }
}
