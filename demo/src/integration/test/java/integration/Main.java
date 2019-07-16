package integration;

import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

class Main {
  public static void main(String... args) {
    System.out.println(Main.class.getModule().getDescriptor());

    var listener = new SummaryGeneratingListener();

    var launcherConfig =
        LauncherConfig.builder()
            // .enableTestEngineAutoRegistration(false)
            // .enableTestExecutionListenerAutoRegistration(false)
            // .addTestEngines(new CustomTestEngine())
            // .addTestExecutionListeners(new LegacyXmlReportGeneratingListener(reportsDir, out))
            // .addTestExecutionListeners(new CustomTestExecutionListener())
            .addTestExecutionListeners(listener)
            .build();

    var launcher = LauncherFactory.create(launcherConfig);

    var request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectModule(Main.class.getModule().getName()))
            // .filters(TagFilter.includeTags("fast"))
            .build();

    launcher.execute(request);

    var summary = listener.getSummary();
    if (summary.getTotalFailureCount() != 0) {
      summary.printFailuresTo(new PrintWriter(System.err, true));
      throw new AssertionError(summary.getTotalFailureCount() + " failure(s)...");
    }
  }
}
