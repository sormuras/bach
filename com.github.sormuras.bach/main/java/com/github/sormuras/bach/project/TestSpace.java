package com.github.sormuras.bach.project;

import java.util.List;

/** Test code space. */
public record TestSpace(List<String> modules, ModulePaths modulePaths, Tweaks tweaks)
    implements Space {
  @Override
  public String name() {
    return "test";
  }
}
