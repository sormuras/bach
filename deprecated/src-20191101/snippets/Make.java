import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** "Make it so!" */
public class Make {

  public static void main(String... args) throws Exception {
    System.out.printf("Make.java %s%n", List.of(args));
    var mains = List.of("run(\"123\");");
    var template = Files.readAllLines(Path.of(args[0]));
    var lines = new ArrayList<String>();
    for (var line : template) {
      int mainIndex = line.indexOf("/*main.block {}*/");
      if (mainIndex >= 0) {
        var indent = line.substring(0, mainIndex);
        mains.forEach(main -> lines.add(indent + main));
        continue;
      }
      lines.add(line.replaceAll("\\Q/*class.name {*/\\E.+\\Q/*} class.name*/\\E", "Build"));
    }
    lines.forEach(System.out::println);
  }

  final Log log;

  Make(Log log) {
    this.log = log;
  }

  public void run(Command command) {
    var code = command.run(log);
    if (code != 0) {
      throw new AssertionError("Non-zero exit code: " + code);
    }
  }

  /** Simple logging facility. */
  static class Log implements AutoCloseable {

    static Log ofSystem() {
      return new Log(new PrintWriter(System.out), new PrintWriter(System.err));
    }

    static Log of(Writer writer) {
      return new Log(new PrintWriter(writer), new PrintWriter(writer));
    }

    final PrintWriter out;
    final PrintWriter err;

    Log(PrintWriter out, PrintWriter err) {
      this.out = out;
      this.err = err;
    }

    @Override
    public void close() {
      out.flush();
      out.close();
      err.flush();
      err.close();
    }
  }

  /** Batch of commands. */
  static class Batch extends Command {

    enum Mode {
      SEQUENTIAL,
      PARALLEL
    }

    final List<? extends Command> commands;
    final Mode mode;

    Batch(String name, Mode mode, List<? extends Command> commands) {
      super(name, List.of());
      this.mode = mode;
      this.commands = commands;
    }

    @Override
    int run(Log log) {
      var stream = mode == Mode.SEQUENTIAL ? commands.stream() : commands.parallelStream();
      return stream.mapToInt(command -> command.run(log)).sum();
    }
  }

  /** {@code javac} and arguments. */
  static class Command {
    final String name;
    final List<String> arguments;

    Command(String name, List<String> arguments) {
      this.name = name;
      this.arguments = arguments;
    }

    int run(Log log) {
      return 0;
    }
  }
}
