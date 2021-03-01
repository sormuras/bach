package com.github.sormuras.bach.project;

/** A nominal space for modular Java source code. */
public interface Space {

  /** {@return the name of the code space} */
  String name();

  /** {@return the additional arguments to be passed on a per-tool basis} */
  Tweaks tweaks();
}
