package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.project.ModuleLookup;
import java.util.Optional;

/** An always empty-returning module lookup. */
public class EmptyModuleLookup implements ModuleLookup {

  /** An always empty-returning module lookup. */
  public static final ModuleLookup SINGLETON = new EmptyModuleLookup();

  /** Hidden default constructor. */
  private EmptyModuleLookup() {}

  @Override
  public Optional<String> lookup(String module) {
    return Optional.empty();
  }
}
