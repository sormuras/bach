package com.github.sormuras.bach.command;

import java.util.Optional;

/**
 * An option holding an optional {@code Boolean} value.
 *
 * @param value the state of this flag
 */
public record VerboseOption(Optional<Boolean> value) implements Option.Flag {
  public static VerboseOption empty() {
    return new VerboseOption(Optional.empty());
  }
}
