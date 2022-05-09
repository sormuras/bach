package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Test implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var tester = new Tester(bach, out, err);
    tester.runSpaceLauncher();
    tester.runJUnitPlatform();
    return 0;
  }

  record Tester(Bach bach, PrintWriter out, PrintWriter err) {
    void runSpaceLauncher() {
      var launcher = bach.project().spaces().test().launcher();
      if (launcher.isEmpty()) return;
      var paths = bach.configuration().paths();
      var java =
          ToolCall.of("java")
              .with("--module-path", paths.out("test", "modules"))
              .with("--module", launcher.get());
      bach.run(java);
    }

    void runJUnitPlatform() {}
  }
}
