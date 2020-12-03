package com.github.sormuras.bach.project;

import java.lang.module.ModuleFinder;

/**
 * A set of well-known code spaces.
 *
 * @param main the main code space
 * @param test the test code space
 */
public record CodeSpaces(MainCodeSpace main, TestCodeSpace test) {

  /** @return {@code true} if all spaces are empty, else {@code false} */
  public boolean isEmpty() {
    return main.modules().isEmpty() && test.modules().isEmpty();
  }

  /** @return a new module finder composed of all code spaces */
  public ModuleFinder finder() {
    return ModuleFinder.compose(main.modules(), test.modules());
  }
}
