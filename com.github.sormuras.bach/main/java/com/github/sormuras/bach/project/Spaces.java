package com.github.sormuras.bach.project;

import java.lang.module.ModuleFinder;

/** A record of code spaces and code related properties. */
public record Spaces(JavaStyle style, MainSpace main, TestSpace test) {
  public ModuleFinder toModuleFinder() {
    return ModuleFinder.compose(main.declarations(), test.declarations());
  }
}
