package com.github.sormuras.bach.command;

import java.nio.file.Path;
import java.util.Optional;

/**
 * An option holding the default place where to store generated files.
 *
 * <p>For example, {@code javac} stores generated class files; and {@code javadoc} stores generated
 * HTML files in this directory.
 *
 * @param value Specify where to place generated files.
 */
public record OutputDirectoryOption(Optional<Path> value) implements Option.Value<Path> {
  public static OutputDirectoryOption empty() {
    return new OutputDirectoryOption(Optional.empty());
  }
}
