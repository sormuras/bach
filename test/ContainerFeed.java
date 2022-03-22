import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class ContainerFeed implements TestExecutionListener {

  private record Summary(int size, int successful, int aborted, int failed, int skipped) {
    Summary with(Summary other) {
      return new Summary(
          size + other.size,
          successful + other.successful,
          aborted + other.aborted,
          failed + other.failed,
          skipped + other.skipped);
    }
  }

  private final Map<String, Summary> results = new ConcurrentHashMap<>();

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    printCaptions();
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    printCaptions();
  }

  @Override
  public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
    if (identifier.isTest()) {
      var status = result.getStatus();
      var summary =
          new Summary(
              1,
              status == TestExecutionResult.Status.SUCCESSFUL ? 1 : 0,
              status == TestExecutionResult.Status.ABORTED ? 1 : 0,
              status == TestExecutionResult.Status.FAILED ? 1 : 0,
              0);
      merge(identifier, summary);
    }
    if (identifier.isContainer()) {
      var summary = results.get(identifier.getUniqueId());
      if (summary == null) return;
      print(identifier, summary);
    }
  }

  @Override
  public void executionSkipped(TestIdentifier identifier, String reason) {
    if (identifier.isTest()) {
      var summary = new Summary(1, 0, 0, 0, 1);
      merge(identifier, summary);
    }
  }

  private void merge(TestIdentifier identifier, Summary summary) {
    var joiner = new StringJoiner("/");
    var segments = identifier.getUniqueIdObject().getSegments();
    for (var segment : segments) {
      joiner.add('[' + segment.getType() + ':' + segment.getValue() + ']');
      results.merge(joiner.toString(), summary, Summary::with);
    }
  }

  private void printCaptions() {
    System.out.printf(
        "%-25s %9s %9s %9s %9s %9s%n", "Name", "Found", "OK", "Aborted", "Failed", "Skipped");
  }

  private void print(TestIdentifier identifier, Summary summary) {
    System.out.printf(
        "%-25s %9s %9s %9s %9s %9s%n",
        identifier.getDisplayName(),
        summary.size,
        summary.successful,
        summary.aborted,
        summary.failed,
        summary.skipped);
  }
}
