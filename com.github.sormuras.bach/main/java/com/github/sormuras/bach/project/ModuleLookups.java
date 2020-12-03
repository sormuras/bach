package com.github.sormuras.bach.project;

/** A namespace for pre-defined module lookup implementations. */
public interface ModuleLookups {

  /** Maps well-known LWJGL 3.2.3 modules to their Maven Central artifacts. */
  class LWJGL_3_2_3 extends ModuleLookup.LWJGL {
    /** Default constructor. */
    public LWJGL_3_2_3() {
      super("3.2.3");
    }
  }

  /** Maps well-known JUnit Jupiter 5.7.0 modules to their Maven Central artifacts. */
  class JUnitJupiter_5_7 extends ModuleLookup.JUnitJupiter {

    /** Default constructor. */
    public JUnitJupiter_5_7() {
      super("5.7.0");
    }
  }

  /** Maps well-known JUnit Platform 1.7.0 modules to their Maven Central artifacts. */
  class JUnitPlatform_1_7 extends ModuleLookup.JUnitPlatform {

    /** Constructs a new module searcher with the given version. */
    public JUnitPlatform_1_7() {
      super("1.7.0");
    }
  }
}
