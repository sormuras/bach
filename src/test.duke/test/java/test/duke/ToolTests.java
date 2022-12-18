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
    testIdentifiers();
    testInvalidIdentifiers();
    testNamespace();
    testNickname();
    testMatches();
    testCanonical();
    testEmptyNamespaceAndCustomNickname();
    return 0;
  }

  void testIdentifiers() {
    Tool.checkIdentifier("/tool");
    Tool.checkIdentifier("jdk.compiler/javac");
    Tool.checkIdentifier("deep/space/nine");
  }

  void testInvalidIdentifiers() {
    testInvalidIdentifier(null);
    testInvalidIdentifier("");
    testInvalidIdentifier("\t");
    testInvalidIdentifier("/");
    testInvalidIdentifier("tool");
    testInvalidIdentifier("tool/");
  }

  void testInvalidIdentifier(String identifier) {
    try {
      Tool.checkIdentifier(identifier);
      throw new AssertionError("Valid?! " + identifier);
    } catch (RuntimeException expected) {
      // expected
    }
  }

  void testNamespace() {
    assert "".equals(Tool.namespace("tool"));
    assert "jdk.compiler".equals(Tool.namespace("jdk.compiler/javac"));
    assert "deep/space".equals(Tool.namespace("deep/space/nine"));
  }

  void testNickname() {
    assert "tool".equals(Tool.nickname("tool"));
    assert "javac".equals(Tool.nickname("jdk.compiler/javac"));
    assert "nine".equals(Tool.nickname("deep/space/nine"));
  }

  void testMatches() {
    assert Tool.matches("/tool", "/tool");
    assert Tool.matches("/tool", "tool");
    assert Tool.matches("jdk.compiler/javac", "jdk.compiler/javac");
    assert Tool.matches("jdk.compiler/javac", "javac");
    assert Tool.matches("deep/space/nine", "deep/space/nine");
    assert Tool.matches("deep/space/nine", "nine");
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
    var tool = Tool.of("/0", zero);
    assert "0".equals(tool.nickname());
    assert "".equals(tool.namespace());
    assert "/0".equals(tool.identifier());
    assert tool.test("0");
    assert tool.test("/0");
    assert !tool.test("zero");
  }
}
