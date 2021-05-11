package test.base;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record TestProvider1() implements ToolProvider {

  @Override
  public String name() {
    return "test";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      assertThatAssertionsAreEnabled();
      return 0;
    } catch (Throwable throwable) {
      throwable.printStackTrace(err);
      return 1;
    }
  }

  private void assertThatAssertionsAreEnabled() {
    try {
      assert false;
    } catch (AssertionError expected) {
      return;
    }
    throw new AssertionError("Assertions are not enabled for " + getClass().getClassLoader());
  }
}
