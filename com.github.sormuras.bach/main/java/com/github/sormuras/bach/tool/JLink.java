package com.github.sormuras.bach.tool;

import com.github.sormuras.bach.Command;
import java.util.List;

public record JLink(List<Argument> arguments) implements Command<JLink> {
  @Override
  public String name() {
    return "jlink";
  }

  @Override
  public JLink arguments(List<Argument> arguments) {
    return new JLink(arguments);
  }
}
