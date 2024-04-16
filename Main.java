/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

class Main {
  public static void main(String... args) {
    if (args.length < 1) {
      System.out.println("Usage: bach [OPTIONS...] COMMAND [ARGS...]");
      return;
    }
    var arguments = new ArrayDeque<>(List.of(args));
    var command = arguments.removeFirst();
    switch (command.toLowerCase()) {
      case "run" -> run(arguments.removeFirst(), arguments);
      case "status" -> System.out.println("TODO");
      default -> run(command, arguments);
    }
  }

  private static void run(String string, Deque<String> arguments) {
    var tool = tool(string);
    tool.run(args -> args.addAll(arguments.stream()));
  }

  private static Tool tool(String string) {
    var separator = string.indexOf('=');
    // <NAME>|<URI>
    if (separator == -1) return Tool.of(string);
    // <ID=URI>
    var identifier = string.substring(0, separator);
    var source = string.substring(separator + 1);
    var installer = ToolInstaller.ofJavaApplication(identifier, source);
    return Tool.of(installer, ToolInstaller.Mode.INSTALL_IMMEDIATE);
  }
}
