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

  final StringWriter out;
  final StringWriter err;

  TestRun() {
    this(new StringWriter(), new StringWriter());
  }

  private TestRun(StringWriter out, StringWriter err) {
    super(System.Logger.Level.ALL, false, new PrintWriter(out), new PrintWriter(err));
    this.out = out;
    this.err = err;
  }

  //  Make make(Path home, Path work) {
  //    var properties = new Properties();
  //    properties.setProperty("debug", "true");
  //    properties.setProperty("work", work.toString());
  //    properties.setProperty("do-launch-junit-platform", "true");
  //    var configuration = new Make.Configuration(home, properties);
  //    return new Make(configuration);
  //  }

  //  TestRun run(int expected, Path home, Path work, String... args) {
  //    var code = make(home, work).run(this, List.of(args));
  //    if (expected == code) {
  //      return this;
  //    }
  //    throw new AssertionError("Run finished with exit code " + code + toString());
  //  }

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
