package com.github.sormuras.bach.project;

/** Main code space. */
public record MainSpace(Tweaks tweaks) implements Space {
  @Override
  public String name() {
    return "main";
  }
}
