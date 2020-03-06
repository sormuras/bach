package de.sormuras.bach.execution;

import java.nio.file.Path;
import java.util.StringJoiner;

/** Snippet producer and default Java source code helpers. */
interface Scribe {

  /** Convert the string representation of the given object into a {@code String} literal. */
  default String $(Object object) {
    if (object == null) return "null";
    return '"' + object.toString().replace("\\", "\\\\") + '"';
  }

  /** Create {@code Path.of("some/path/...")} literal. */
  default String $(Path path) {
    return "Path.of(" + $((Object) path) + ")";
  }

  /** Create {@code "first" [, "second" [, ...]]} literal. */
  default String $(String[] strings) {
    if (strings.length == 0) return "";
    if (strings.length == 1) return $(strings[0]);
    if (strings.length == 2) return $(strings[0]) + ", " + $(strings[1]);
    var joiner = new StringJoiner(", ");
    for(var string : strings) joiner.add($(string));
    return joiner.toString();
  }

  /** Return source code snippet. */
  Snippet toSnippet();
}
