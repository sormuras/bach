package de.sormuras.bach.api;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

/** Single source path with optional release directive. */
public /*static*/ final class Source {

  /** Source-related flags. */
  public enum Flag {
    /** Store binary assets in {@code META-INF/versions/${release}/} directory of the jar. */
    VERSIONED
  }

  /** Create default non-targeted source for the specified path and optional flags. */
  public static Source of(Path path, Flag... flags) {
    return new Source(path, 0, Set.of(flags));
  }

  private final Path path;
  private final int release;
  private final Set<Flag> flags;

  public Source(Path path, int release, Set<Flag> flags) {
    this.path = Objects.requireNonNull(path, "path");
    this.release = release;
    this.flags = flags.isEmpty() ? Set.of() : EnumSet.copyOf(flags);
  }

  /** Source path. */
  public Path path() {
    return path;
  }

  /** Java feature release target number, with zero indicating the current runtime release. */
  public int release() {
    return release;
  }

  /** This source's flags. */
  public Set<Flag> flags() {
    return flags;
  }

  public boolean versioned() {
    return flags.contains(Flag.VERSIONED);
  }

  public boolean targeted() {
    return release != 0;
  }

  /** Optional Java feature release target number. */
  public OptionalInt target() {
    return targeted() ? OptionalInt.of(release) : OptionalInt.empty();
  }
}
