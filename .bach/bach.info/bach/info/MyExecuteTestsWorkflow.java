package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.workflow.ExecuteTestsWorkflow;
import java.util.List;
import java.util.spi.ToolProvider;

public class MyExecuteTestsWorkflow extends ExecuteTestsWorkflow {

  public MyExecuteTestsWorkflow(Bach bach) {
    super(bach);
  }

  @Override
  protected ToolRun runJUnit(ToolProvider provider, String module) {
    var project = bach().project();
    var tweaks = project.tools().tweaks();
    var java =
        new MyJavaCall(List.of())
            .with("--show-version")
            .with("-enableassertions")
            .with("--module-path", computeModulePaths(module))
            .with("--add-modules", module)
            .with("--add-modules", "ALL-DEFAULT")
            .with("--module", "org.junit.platform.console")
            .with("--disable-banner")
            .with("--select-module", module)
            .withAll(tweaks.arguments(CodeSpace.TEST, "junit"))
            .withAll(tweaks.arguments(CodeSpace.TEST, "junit(" + module + ")"))
            .with("--reports-dir", project.folders().workspace("reports", "junit", module));
    return bach().run(java);
  }
}
