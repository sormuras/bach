package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ModuleLookup;
import java.util.Optional;

/** An always {@link Optional#empty() empty} returning module lookup. */
public final class EmptyModuleLookup implements ModuleLookup {

  /** The always {@link Optional#empty() empty} returning module lookup instance. */
  public static final ModuleLookup SINGLETON = new EmptyModuleLookup();

  /** Hidden default constructor. */
  private EmptyModuleLookup() {}

  /**
   * @param module the name of the module to lookup
   * @return always {@code Optional.empty()}
   */
  @Override
  public Optional<String> lookupModule(String module) {
    return Optional.empty();
  }
}
