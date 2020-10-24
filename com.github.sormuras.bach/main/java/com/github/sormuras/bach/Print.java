package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Paths;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/** Print-related API. */
public /*sealed*/ interface Print /*permits Bach*/ {

  /**
   * Returns the print stream for printing messages.
   *
   * @return the print stream for printing messages
   */
  PrintStream printer();

  /**
   * Print a listing of all files matching the given glob pattern.
   *
   * @param glob the glob pattern
   */
  default void printFind(String glob) {
    var matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
    var start = Path.of("");
    try (var stream =
        Files.find(start, 99, (path, bfa) -> Paths.isVisible(path) && matcher.matches(path))) {
      stream.map(Paths::slashed).filter(Predicate.not(String::isEmpty)).forEach(printer()::println);
    } catch (Exception exception) {
      throw new RuntimeException("find failed: " + glob, exception);
    }
  }
}
