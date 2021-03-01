package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.project.Libraries;
import com.github.sormuras.bach.project.Settings;
import com.github.sormuras.bach.project.Spaces;

/**
 * Bach's project model.
 *
 * @param settings fundamental properties
 * @param libraries external module manager
 * @param spaces code spaces
 */
public record Project(Settings settings, Libraries libraries, Spaces spaces) {
  /**
   * {@return the name of this project}
   *
   * @see Settings#name()
   */
  public String name() {
    return settings.name();
  }

  /**
   * {@return a string containing the full version}
   *
   * @see Settings#version()
   */
  public String version() {
    return settings.version().toString();
  }

  /**
   * {@return a string containing only the version number and, if present, the pre-release version}
   *
   * @see Settings#version()
   */
  public String versionNumberAndPreRelease() {
    return Strings.toNumberAndPreRelease(settings.version());
  }
}
