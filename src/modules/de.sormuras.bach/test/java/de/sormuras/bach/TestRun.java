package de.sormuras.bach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class TestRun extends Run {

  /** Force debug mode when running tests. */
  static class TestProperties extends Run.DefaultProperties {
    TestProperties(Path home, Path work) {
      Run.loadProperties(this, home);
      setProperty("home", home.toString());
      setProperty("work", work.toString());
      setProperty("debug", "true");
      setProperty("${test.key}", "test.value");
    }
  }

  /** Walk directory tree structure. */
  static List<String> treeWalk(Path root) {
    var lines = new ArrayList<String>();
    treeWalk(root, lines::add);
    return lines;
  }

  /** Walk directory tree structure. */
  static void treeWalk(Path root, Consumer<String> out) {
    try (var stream = Files.walk(root)) {
      stream
          .map(root::relativize)
          .map(path -> path.toString().replace('\\', '/'))
          .sorted()
          .filter(Predicate.not(String::isEmpty))
          .forEach(out);
    } catch (Exception e) {
      throw new Error("Walking tree failed: " + root, e);
    }
  }

  private final StringWriter out;
  private final StringWriter err;
  private boolean testOffline;

  TestRun() {
    this(Path.of(""), Path.of("target", "test-run"));
  }

  TestRun(Path home, Path work) {
    this(home, work, new StringWriter(), new StringWriter());
  }

  private TestRun(Path home, Path work, StringWriter out, StringWriter err) {
    super(new TestProperties(home, work), new PrintWriter(out, true), new PrintWriter(err, true));
    this.out = out;
    this.err = err;
    this.testOffline = super.isOffline();
  }

  TestRun setOffline(boolean testOffline) {
    this.testOffline = testOffline;
    return this;
  }

  @Override
  public boolean isOffline() {
    return testOffline;
  }

  @Override
  ProcessBuilder newProcessBuilder(String command) {
    return super.newProcessBuilder(command)
        .redirectInput(ProcessBuilder.Redirect.PIPE)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE);
  }

  List<String> outLines() {
    return out.toString().lines().collect(Collectors.toList());
  }

  List<String> errLines() {
    return err.toString().lines().collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return "\n\n___err\n" + err + "\n\n___out\n" + out;
  }
}
