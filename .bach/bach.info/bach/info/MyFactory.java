package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.workflow.ExecuteTestsWorkflow;

public class MyFactory extends Factory {
  @Override
  public ExecuteTestsWorkflow newExecuteTestsWorkflow(Bach bach) {
    return new MyExecuteTestsWorkflow(bach);
  }
}
