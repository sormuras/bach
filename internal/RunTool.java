/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.internal;

import java.util.Arrays;
import run.bach.Tool;
import run.bach.ToolInstaller;

class RunTool {
  public static void main(String... args) {
    if (args.length == 0) {
      System.out.println("Usage: RunTool.java <NAME>|<URI>|<ID=URI> [ARGS...]");
      return;
    }
    tool(args[0]).run(Arrays.copyOfRange(args, 1, args.length));
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
