package com.github.sormuras.bach.project;

import java.util.List;

/** Main code space. */
public record MainSpace(List<String> modules, ModulePaths modulePaths, int release, Tweaks tweaks)
    implements Space {
  @Override
  public String name() {
    return "main";
  }
}
