package test.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Bach;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A project build context. */
class Context {

  final Path base;
  final Path temp;
  final int code;

  Context(String name, Path temp) {
    this(Path.of("test.integration", "project", name), temp, 0);
  }

  Context(String name, Path temp, int code) {
    this(Path.of("test.integration", "broken", name), temp, code);
  }

  Context(Path base, Path temp, int code) {
    this.base = base;
    this.temp = temp;
    this.code = code;
  }

  Path workspace(String first, String... more) {
    return base.resolve(Bach.WORKSPACE).resolve(Path.of(first, more));
  }

  String build(String... options) throws Exception {
    var idea = Path.of(".idea/out/production/com.github.sormuras.bach").toAbsolutePath();
    var work = Path.of(".bach/workspace/modules").toAbsolutePath();
    var cache = (Files.isDirectory(idea) ? idea : work).toString();

    var command = new ArrayList<String>();
    command.add("java");
    command.add("-Dbach.cache=" + cache);
    command.addAll(List.of(options));
    command.add("--module-path");
    command.add(cache);
    command.add("--module");
    command.add("com.github.sormuras.bach/com.github.sormuras.bach.Main");
    command.add("build");
    var redirect = temp.resolve("redirect.txt");
    var process =
        new ProcessBuilder(command)
            .directory(base.toFile())
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

  ModuleFinder newModuleFinder() {
    return ModuleFinder.of(base.resolve(".bach/workspace/modules"));
  }
}
