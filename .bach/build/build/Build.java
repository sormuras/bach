package build;

import com.github.sormuras.bach.Bach;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.spi.ToolProvider;

public class Build implements ToolProvider {

  public static void main(String... args) {
    new Build().run(System.out, System.err, args);
  }

  private static String version() {
    try {
      return Files.readString(Path.of("VERSION"));
    } catch (Exception exception) {
      throw new Error("Read VERSION failed: ", exception);
    }
  }

  public Build() {}

  @Override
  public String name() {
    return "build";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var version = args.length == 0 ? version() : args[0];
    var jarslug = args.length < 2 ? version : args[1];
    out.println("Build Bach " + version + " using Bach " + Bach.version());
    var start = Instant.now();
    try {
      new Simple(out, err, version, jarslug).run();
      return 0;
    } catch (Exception exception) {
      err.println(exception);
      return 1;
    } finally {
      out.printf("Build took %d milliseconds%n", Duration.between(start, Instant.now()).toMillis());
    }
  }

  @Override
  public String toString() {
    return "Bach's Build Program";
  }

  static class Simple {

    final PrintWriter out;
    final PrintWriter err;
    final String version;
    final String jarslug;

    final Path workspace = Path.of(".bach/workspace");

    Simple(PrintWriter out, PrintWriter err, String version, String jarslug) {
      this.out = out;
      this.err = err;
      this.version = version;
      this.jarslug = jarslug;
    }

    void run() throws Exception {
      var module = "com.github.sormuras.bach";
      var classes = workspace.resolve("classes/" + Runtime.version().feature());
      var modules = Files.createDirectories(workspace.resolve("modules"));
      var file = modules.resolve(module + "@" + jarslug + ".jar");

      run(
          Command.builder("javac")
              .with("--module", module)
              .with("--module-source-path", "./*/main/java")
              .with("--release", 16)
              .with("-g")
              .with("-parameters")
              .with("-Werror")
              .with("-Xlint")
              .with("-encoding", "UTF-8")
              .with("-d", classes)
              .build());

      run(
          Command.builder("jar")
              .with("--create")
              .with("--file", file)
              .with("--main-class", module + ".Main")
              .with("--module-version", version)
              .with("-C", classes.resolve(module), ".")
              .with("-C", Path.of(module, "main", "java"), ".")
              .build());

      var api = workspace.resolve("documentation/api");
      run(
          Command.builder("javadoc")
              .with("--module", module)
              .with("--module-source-path", "./*/main/java")
              .with("-windowtitle", "\uD83C\uDFBC Bach " + version)
              .with("-header", "\uD83C\uDFBC Bach " + version)
              .with("-footer", "\uD83C\uDFBC Bach " + version)
              .with("-use")
              .with("-linksource")
              .with("-notimestamp")
              .with("-Werror")
              .with("-Xdoclint")
              .with("-encoding", "UTF-8")
              .with("-quiet")
              .with("-d", api)
              .build());

      run(
          Command.builder("jar")
              .with("--create")
              .with("--file", api.getParent().resolve("bach-api-" + version + ".zip"))
              .with("--no-manifest")
              .with("-C", api, ".")
              .build());
    }

    void run(Command command) {
      out.printf("<< %s%n", command);
      var tool = ToolProvider.findFirst(command.name()).orElseThrow();
      var normal = new StringWriter();
      var errors = new StringWriter();
      var code = tool.run(new PrintWriter(normal), new PrintWriter(errors), command.args());

      if (!normal.getBuffer().isEmpty()) out.println(normal.toString().indent(3).stripTrailing());
      if (!errors.getBuffer().isEmpty()) err.println(errors);

      if (code != 0) throw new Error(tool.name() + " returned error code: " + code);
    }
  }

  /** An immutable tool call implementation. */
  static final class Command {

    /**
     * Instantiates a builder to build a command.
     *
     * @param name the name of the tool
     * @return a new builder
     */
    public static Builder builder(String name) {
      return new Builder(name);
    }

    /**
     * Builds a command with the given tool name and an array of arguments.
     *
     * @param name the name of the tool
     * @param arguments the arguments
     * @return a new command
     */
    public static Command of(String name, Object... arguments) {
      var builder = new Builder(name);
      for (var argument : arguments) builder.with(argument);
      return builder.build();
    }

    private final String name;
    private final String[] args;

    /**
     * Initializes a new command with a name and an array of arguments.
     *
     * @param name the name of the tool to call
     * @param args the arguments
     */
    public Command(String name, String... args) {
      this.name = name;
      this.args = args;
    }

    public String name() {
      return name;
    }

    public String[] args() {
      return args;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Command command = (Command) o;
      return name.equals(command.name) && Arrays.equals(args, command.args);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(name);
      result = 31 * result + Arrays.hashCode(args);
      return result;
    }

    @Override
    public String toString() {
      return args.length == 0 ? name : name + ' ' + String.join(" ", args);
    }

    /**
     * A builder for building {@link Command} objects.
     *
     * <p>Example usage:
     *
     * <pre>{@code
     * var command = Command.builder("name")
     *     .with("first-argument")
     *     .with("option", "value")
     *     .build();
     * }</pre>
     *
     * <p>A Builder checks the components and invariants as components are added to the builder. The
     * rationale for this is to detect errors as early as possible and not defer all validation to
     * the {@link #build()} method.
     */
    public static final class Builder {

      private final String name;
      private final List<String> strings;

      /**
       * Initializes a new builder with the given tool name.
       *
       * @param name the name of the tool
       */
      Builder(String name) {
        this.name = name;
        this.strings = new ArrayList<>();
      }

      /**
       * Builds and returns a command from its components.
       *
       * @return the command object
       */
      public Command build() {
        return new Command(name, strings.toArray(String[]::new));
      }

      /**
       * Adds an argument.
       *
       * @param argument the argument
       * @return this builder instance
       * @throws IllegalArgumentException If the string representation of the argument is blank
       */
      public Builder with(Object argument) {
        var string = Objects.requireNonNull(argument, "argument must not be null").toString();
        if (string.isBlank()) throw new IllegalArgumentException("argument must not be blank");

        strings.add(string);
        return this;
      }

      /**
       * Adds two or more arguments.
       *
       * @param option the first argument
       * @param value the second argument
       * @param values the second argument
       * @return this builder instance
       */
      public Builder with(String option, Object value, Object... values) {
        Objects.requireNonNull(option, "option must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(values, "values must not be null");

        with(option).with(value);
        for (var more : values) with(more);
        return this;
      }
    }
  }
}
