import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandTests {
  private static void dump() {
    List<String> strings = new ArrayList<>();
    Bach.Command command = new Bach().new Command("executable");
    command.add("--some-option").add("value");
    command.add("-single-flag-without-values");
    command.markDumpLimit(5);
    command.addAll("0", "1", "2", "3", "4");
    command.addAll("5", "6", "7", "8", "9");
    command.dumpToPrinter((format, args) -> strings.add(String.format('|' + format, args).trim()));
    assert Objects.equals("|executable", strings.get(0));
    assert Objects.equals("|--some-option", strings.get(1));
    assert Objects.equals("|  value", strings.get(2));
    assert Objects.equals("|-single-flag-without-values", strings.get(3));
    assert Objects.equals("|0", strings.get(4));
    assert Objects.equals("|1", strings.get(5));
    assert Objects.equals("|2", strings.get(6));
    assert Objects.equals("|3", strings.get(7));
    assert Objects.equals("|4", strings.get(8));
    assert Objects.equals("|... [omitted 4 arguments]", strings.get(9));
    assert Objects.equals("|9", strings.get(10));
  }

  private static void addAllJavaFiles() {
    Bach.Command command = new Bach().new Command("executable");
    command.addAllJavaFiles(Paths.get("src"));
    command.addAllJavaFiles(Paths.get("test"));
    List<String> arguments = List.of(command.toArgumentsArray());
    assert arguments.size() > 0;
  }

  private static void toolJavacOptions() {
    List<String> strings = new ArrayList<>();
    Bach bach = new Bach();
    Bach.Tool.JavacOptions options = bach.tool.new JavacOptions();
    options.additionalArguments = List.of("-J-Da=b");
    options.deprecation = true;
    options.encoding = StandardCharsets.US_ASCII;
    options.failOnWarnings = true;
    options.parameters = true;
    options.verbose = true;
    assert options.encoding().size() == 2;
    assert options.modulePaths().size() == 2;
    Bach.Command command = bach.command("javac");
    command.addOptions(options);
    command.dumpToPrinter((format, args) -> strings.add(String.format('|' + format, args).trim()));
    assert Objects.equals("|javac", strings.get(0));
    assert Objects.equals("|-J-Da=b", strings.get(1));
    assert Objects.equals("|-deprecation", strings.get(2));
    assert Objects.equals("|-encoding", strings.get(3));
    assert Objects.equals("|  US-ASCII", strings.get(4));
    assert Objects.equals("|-Werror", strings.get(5));
    assert Objects.equals("|--module-path", strings.get(6));
    assert Objects.equals("|  .bach" + File.separator + "dependencies", strings.get(7));
    assert Objects.equals("|-parameters", strings.get(8));
    assert Objects.equals("|-verbose", strings.get(9));
  }

  private static void toolJavadocOptions() {
    List<String> strings = new ArrayList<>();
    Bach bach = new Bach();
    Bach.Tool.JavadocOptions options = bach.tool.new JavadocOptions();
    options.quiet = true;
    Bach.Command command = bach.command("javadoc");
    command.addOptions(options);
    command.dumpToPrinter((format, args) -> strings.add(String.format('|' + format, args).trim()));
    assert Objects.equals("|javadoc", strings.get(0));
    assert Objects.equals("|-quiet", strings.get(1));
  }

  private static void toolJarOptions() {
    List<String> strings = new ArrayList<>();
    Bach bach = new Bach();
    Bach.Tool.JarOptions options = bach.tool.new JarOptions();
    options.noCompress = true;
    options.verbose = true;
    Bach.Command command = bach.command("jar");
    command.addOptions(options);
    command.dumpToPrinter((format, args) -> strings.add(String.format('|' + format, args).trim()));
    assert Objects.equals("|jar", strings.get(0));
    assert Objects.equals("|--no-compress", strings.get(1));
    assert Objects.equals("|--verbose", strings.get(2));
  }

  public static void main(String[] args) {
    dump();
    addAllJavaFiles();
    toolJavacOptions();
    toolJavadocOptions();
    toolJarOptions();
  }
}
