package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.core.ExecuteTestsAction;

public class MyFactory extends Factory {
  @Override
  public ExecuteTestsAction newExecuteTestsAction(Bach bach) {
    return new MyExecuteTestsAction(bach);
  }
}
