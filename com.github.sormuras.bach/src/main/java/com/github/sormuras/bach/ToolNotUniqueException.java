package com.github.sormuras.bach;

import java.util.List;
import java.util.stream.Collectors;

/** An exception thrown to indicate that multiple tools were found for a given name. */
public final class ToolNotUniqueException extends RuntimeException {
  @java.io.Serial private static final long serialVersionUID = -4475718006846080166L;

  ToolNotUniqueException(String name, List<Tool> tools) {
    super(
        """
                Multiple tools found for `%s`:
                  - %s
                """
            .formatted(name, tools.stream().map(Tool::name).collect(Collectors.joining("\n  - ")))
            .stripTrailing());
  }
}
