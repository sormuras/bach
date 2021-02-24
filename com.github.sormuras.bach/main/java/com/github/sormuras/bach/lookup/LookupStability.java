package com.github.sormuras.bach.lookup;

/**
 * A classification of the stability of a module lookup implementation.
 *
 * @see ModuleLookup
 */
public enum LookupStability {
  /** The stability of the lookup is not defined by the implementation. */
  UNKNOWN,

  /** The module lookup always returns the same URI for a given module name. */
  STABLE,

  /** The module lookup may return different URIs for a given module name. */
  DYNAMIC
}
