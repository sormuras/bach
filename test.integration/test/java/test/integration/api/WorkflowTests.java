package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.api.Workflow;
import com.github.sormuras.bach.api.UnsupportedWorkflowException;
import org.junit.jupiter.api.Test;

class WorkflowTests {
  @Test
  void factoryThrowsOnUnsupportedAction() {
    assertThrows(UnsupportedWorkflowException.class, () -> Workflow.ofCli(""));
  }
}
