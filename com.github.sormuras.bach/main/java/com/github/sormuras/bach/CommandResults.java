package com.github.sormuras.bach;

import java.util.List;

/** A multi-result collector. */
public record CommandResults(List<CommandResult> list) {

  /**
   * Returns silently if all recorded represent successful tool runs.
   *
   * @throws RuntimeException if any recording failed
   */
  public void requireSuccessful() {
    var errors = list.stream().filter(CommandResult::isError).toList();
    if (errors.isEmpty()) return;
    if (errors.size() == 1) errors.get(0).requireSuccessful();
    throw new RuntimeException(errors.size() + " runs returned a non-zero code");
  }
}
