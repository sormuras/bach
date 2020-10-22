package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import java.nio.file.Path;

/** Java Shell Builder. */
public class Bach {
  /**
   * Returns the version.
   *
   * @return the version or {@code "?"} if the version is unknown at runtime
   */
  public static String version() {
    var descriptor = Bach.class.getModule().getDescriptor();
    if (descriptor == null) return "?";
    return descriptor.version().map(Object::toString).orElse("?");
  }

  public static void tree(String... paths) {
    Paths.walk(Path.of("", paths), System.out::println);
  }

  /** Hidden default constructor. */
  private Bach() {}
}
