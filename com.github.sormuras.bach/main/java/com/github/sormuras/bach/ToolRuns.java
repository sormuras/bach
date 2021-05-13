package com.github.sormuras.bach;

import com.github.sormuras.bach.api.BachException;
import java.util.List;

/** A multi-run collector. */
public record ToolRuns(List<ToolRun> list) {

  /**
   * Returns silently if all represent successful tool call runs.
   *
   * @throws BachException if one or more more tool call runs signal an error
   */
  public void requireSuccessful() {
    var errors = list.stream().filter(ToolRun::isError).toList();
    if (errors.isEmpty()) return;
    if (errors.size() == 1) errors.get(0).requireSuccessful();
    throw new BachException("%d runs returned a non-zero code", errors.size());
  }
}
