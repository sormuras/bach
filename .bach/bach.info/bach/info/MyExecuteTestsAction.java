package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.CommandResult;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.core.ExecuteTestsAction;
import java.util.List;
import java.util.spi.ToolProvider;

public class MyExecuteTestsAction extends ExecuteTestsAction {

  public MyExecuteTestsAction(Bach bach) {
    super(bach);
  }

  @Override
  public CommandResult runJUnit(ToolProvider provider, String module) {
    var project = bach().project();
    var tweaks = project.tools().tweaks();

    var folders = bach().project().folders();
    var modulePaths =
        List.of(
            folders.jar(CodeSpace.TEST, module, bach().project().version()), // module under test
            folders.modules(CodeSpace.MAIN), // main modules
            folders.modules(CodeSpace.TEST), // (more) test modules
            folders.externals());

    var java =
        new MyJava(List.of())
            .with("--show-version")
            .with("-enableassertions")
            .with("--module-path", modulePaths)
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
