package com.github.sormuras.bach.internal;

import java.util.ArrayList;
import java.util.List;

public class MainArguments {

  /**
   * {@code bach [options...] [actions...]}
   */
  public static MainArguments of(String... args) {
    var actions = new ArrayList<String>();
    var verbose = false;
    for (var arg : args) {
      if (actions.isEmpty() && arg.startsWith("-")) {
        if (arg.equals("--verbose")) {
          verbose = true;
          continue;
        }
        throw new IllegalArgumentException("Unsupported option: " + arg);
      }
      actions.add(arg);
    }
    return new MainArguments(verbose, List.copyOf(actions));
  }

  private final boolean verbose;
  private final List<String> actions;

  public MainArguments(boolean verbose, List<String> actions) {
    this.verbose = verbose;
    this.actions = actions;
  }

  public boolean verbose() {
    return verbose;
  }

  public List<String> actions() {
    return actions;
  }
}
