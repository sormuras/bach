package com.github.sormuras.bach;

import java.util.List;

/** A multi-recordings collector. */
public record Recordings(List<Recording> values) {

  /**
   * Returns silently if all recordings represent successful tool runs.
   *
   * @throws RuntimeException if any recording failed
   */
  public void requireSuccessful() {
    var errors = values.stream().filter(Recording::isError).toList();
    if (errors.isEmpty()) return;
    if (errors.size() == 1) errors.get(0).requireSuccessful();
    throw new RuntimeException(errors.size() + " errors recorded: " + errors);
  }
}
