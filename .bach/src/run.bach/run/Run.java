package run;

import java.util.Arrays;
import run.bach.Tool;
import run.bach.ToolFinder;
import run.bach.ToolInstaller;

class Run {
  public static void main(String... args) {
    if (args.length == 0) {
      System.out.println("Usage: Run TOOL[=URI] [ARGS...]");
      return;
    }

    var tool = args[0];
    var range = Arrays.copyOfRange(args, 1, args.length);
    var index = tool.indexOf('=');
    if (index == -1) {
      Tool.of(tool).run(range);
      return;
    }

    var identifier = Tool.Identifier.of(tool.substring(0, index));
    var source = tool.substring(index + 1);
    ToolFinder.ofInstaller(ToolInstaller.Mode.INSTALL_ON_DEMAND)
        .withJavaApplication(identifier.toNamespaceAndNameAndVersion(), source)
        .findToolOrElseThrow(identifier.name())
        .run(range);
  }
}
