package integration;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;

import java.io.PrintWriter;
import java.util.List;
import java.util.spi.ToolProvider;
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
    return moduleName;
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var arguments = List.of(args);
    out.printf("Running tests in: %s%n", arguments);
    var launcher = LauncherFactory.create();
    var request = LauncherDiscoveryRequestBuilder.request();
    arguments.forEach(arg -> request.selectors(selectModule(arg)));
    var summaryGeneratingListener = new SummaryGeneratingListener();

    launcher.execute(request.build(), summaryGeneratingListener);

    var summary = summaryGeneratingListener.getSummary();
    if (summary.getTotalFailureCount() != 0) {
      summary.printFailuresTo(err);
      summary.printTo(err);
      return 1;
    }
    if (summary.getTestsFoundCount() == 0) {
      err.printf("No tests found in: %s%n", arguments);
      return 2;
    }
    summary.printTo(out);
    return 0;
  }
}
