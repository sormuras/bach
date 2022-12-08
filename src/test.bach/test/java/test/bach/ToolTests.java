package test.bach;

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
    testIllegalToolNamespaces(null, "\t", "/", "//", "/namespace", "namespace/");
    testIllegalToolNicknames(null, "", "\t", "/", "//", "/name", "name/");
    return 0;
  }

  void testCanonical() {
    var zero = new MockToolProvider("zero", 0);
    var tool = new Tool(zero);
    assert "zero".equals(tool.nickname());
    assert "test.bach".equals(tool.namespace());
    assert "test.bach/zero".equals(tool.identifier());
    assert zero == tool.provider();
    assert tool.test("zero");
    assert tool.test("test.bach/zero");
    assert !tool.test("tool");
    assert !tool.test("test.bach/tool");
  }

  void testEmptyNamespaceAndCustomNickname() {
    var zero = new MockToolProvider("zero", 0);
    var tool = new Tool("", "0", zero);
    assert "0".equals(tool.nickname());
    assert "".equals(tool.namespace());
    assert "0".equals(tool.identifier());
    assert tool.test("0");
    assert !tool.test("zero");
  }

  void testIllegalToolNamespaces(String... namespaces) {
    for (var namespace : namespaces) {
      try {
        new Tool(namespace, "tool", new MockToolProvider("mock-1", -1));
      } catch (IllegalArgumentException expected) {
        continue;
      }
      throw new AssertionError("Expected tool namespace to be illegal: " + namespace);
    }
  }

  void testIllegalToolNicknames(String... names) {
    for (var name : names) {
      try {
        new Tool("", name, new MockToolProvider("mock-1", -1));
      } catch (IllegalArgumentException expected) {
        continue;
      }
      throw new AssertionError("Expected tool nickname to be illegal: " + name);
    }
  }
}
