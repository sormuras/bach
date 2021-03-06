package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A project build context. */
class CLI {

  final Path root;
  final Path temp;
  final int code;

  CLI(String name, Path temp) {
    this(Path.of("test.projects", name), temp, 0);
  }

  CLI(Path root, Path temp, int code) {
    this.root = root;
    this.temp = temp;
    this.code = code;
  }

  Path workspace(String first, String... more) {
    return root.resolve(".bach/workspace").resolve(Path.of(first, more));
  }

  String build(String... options) throws Exception {
    var idea = Path.of(".idea/out/production/com.github.sormuras.bach").toAbsolutePath();
    var work = Path.of(".bach/workspace/modules").toAbsolutePath();
    var bin = (Files.isDirectory(idea) ? idea : work).toString();

    var command = new ArrayList<String>();
    command.add("java");
    command.add("--module-path");
    command.add(bin);
    command.add("--module");
    command.add("com.github.sormuras.bach/com.github.sormuras.bach.Main");
    command.addAll(List.of(options));
    command.add("build");
    var redirect = temp.resolve("redirect.txt");
    var process =
        new ProcessBuilder(command)
            .directory(root.toFile())
            .redirectErrorStream(true)
            .redirectOutput(redirect.toFile())
            .start();
    var status = process.waitFor();
    process.destroy();
    var output = Files.readString(redirect);
    var summary =
        process
            + "\nCommand\n"
            + String.join("\n", command).indent(2)
            + "Output\n"
            + output.indent(2);
    assertEquals(code, status, summary);
    return output;
  }
}
