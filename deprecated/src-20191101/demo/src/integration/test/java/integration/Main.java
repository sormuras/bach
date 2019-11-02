package integration;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public class Main implements ToolProvider {

  public static void main(String... args) {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    try {
      var code = new Main().run(out, err, args);
      if (code != 0) {
        throw new AssertionError("run() return non-zero exit code: " + code);
      }
    } catch (Throwable t) {
      throw new Error("run() failed", t);
    }
  }

  private final String moduleName = getClass().getModule().getName();

  @Override
  public String name() {
    return "test(" + moduleName + ")";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.printf("%nRunning tests in module %s%n", moduleName);
    var launcher = LauncherFactory.create();
    var request = LauncherDiscoveryRequestBuilder.request().selectors(selectModule(moduleName));
    var printer = new PrintingListener(out, err);
    var summaryGenerator = new SummaryGeneratingListener();

    launcher.execute(request.build(), printer, summaryGenerator);

    var summary = summaryGenerator.getSummary();
    if (summary.getTotalFailureCount() != 0) {
      summary.printFailuresTo(err);
      summary.printTo(err);
      return 1;
    }
    if (summary.getTestsFoundCount() == 0) {
      err.printf("No tests found in: %s%n", moduleName);
      return 2;
    }
    summary.printTo(out);
    return 0;
  }

  private static class PrintingListener implements TestExecutionListener {

    private final PrintWriter out, err;

    private PrintingListener(PrintWriter out, PrintWriter err) {
      this.out = out;
      this.err = err;
    }

    @Override
    public void executionSkipped(TestIdentifier test, String reason) {
      out.printf(" X %s - %s%n", test.getDisplayName(), reason);
    }

    @Override
    public void executionStarted(TestIdentifier test) {
      if (test.getType().isContainer()) {
        return;
      }
      out.printf(" > %s%n", test.getDisplayName());
    }

    @Override
    public void executionFinished(TestIdentifier test, TestExecutionResult result) {
      if (result.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
        var message = result.getThrowable().map(Throwable::getMessage).orElse(result.toString());
        err.printf(" < %s%n", message);
      }
    }
  }
}
