package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.duke.Tool;

@Registered
@Enabled
public class ToolTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    testCanonical();
    testEmptyNamespaceAndCustomNickname();
    return 0;
  }

  void testCanonical() {
    var zero = new MockToolProvider("zero", 0);
    var tool = Tool.of(zero);
    assert "zero".equals(tool.nickname());
    assert "test.duke".equals(tool.namespace());
    assert "test.duke/zero".equals(tool.identifier());
    assert zero == ((Tool.OfProvider) tool).provider();
    assert tool.test("zero");
    assert tool.test("test.duke/zero");
    assert !tool.test("tool");
    assert !tool.test("test.duke/tool");
  }

  void testEmptyNamespaceAndCustomNickname() {
    var zero = new MockToolProvider("zero", 0);
    var tool = Tool.of("0", zero);
    assert "0".equals(tool.nickname());
    assert "".equals(tool.namespace());
    assert "0".equals(tool.identifier());
    assert tool.test("0");
    assert !tool.test("zero");
  }
}
