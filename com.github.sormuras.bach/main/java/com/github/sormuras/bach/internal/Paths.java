package com.github.sormuras.bach.internal;

import java.nio.file.Files;
import java.nio.file.Path;

/** Internal {@link Path}-related utilities. */
public class Paths {

  public static boolean isViewSupported(Path file, String view) {
    return file.getFileSystem().supportedFileAttributeViews().contains(view);
  }

  public static boolean isVisible(Path path) {
    try {
      for (int endIndex = 1; endIndex <= path.getNameCount(); endIndex++) {
        var subpath = path.subpath(0, endIndex);
        if (Files.isHidden(subpath)) return false;
      }
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String slashed(Path path) {
    return path.toString().replace('\\', '/');
  }

  /** Hidden default constructor. */
  private Paths() {}
}
