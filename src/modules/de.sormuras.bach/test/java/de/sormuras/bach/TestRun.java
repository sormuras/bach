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
    TestProperties(Path home) {
      setProperty("home", home.toString());
      setProperty("debug", "true");
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

  TestRun() {
    this(Path.of(""));
  }

  TestRun(Path home) {
    this(home, new StringWriter(), new StringWriter());
  }

  private TestRun(Path home, StringWriter out, StringWriter err) {
    super(new TestProperties(home), new PrintWriter(out), new PrintWriter(err));
    this.out = out;
    this.err = err;
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
