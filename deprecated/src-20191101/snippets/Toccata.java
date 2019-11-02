/*
 * Bach - Toccata
 *
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Generate Java program building a modular Java project. */
public class Toccata {
  static final String VERSION = "1-ea";

  public static void main(String... args) {
    System.out.println("Toccata " + VERSION);
    var base = Path.of(System.getProperty("user.dir"));
    var actions = build(base);
    new Writer(Path.of("Build.java")).accept(actions);
  }

  static List<Action> build(Path base) {
    var actions = new ArrayList<Action>();

    var javac =
        new Action(
            "run",
            "javac",
            List.of(
                Argument.of("-d"),
                Argument.of(Path.of("bin", "javac")),
                Argument.of("--module-source-path"),
                Argument.of(
                    ".replace(\"{MODULE}\", \"*\")",
                    Path.of("src/{MODULE}/main/java"),
                    Path.of("src/{MODULE}/test/java")),
                Argument.of("--module"),
                Argument.of("de.sormuras.bach.air,it"),
                Argument.of("--module-path"),
                Argument.of("lib")));

    actions.add(javac);

    return actions;
  }

  static class Argument {

    static Argument of(Object object) {
      var string = object.toString();
      var escaped = new StringBuilder();
      for (int i = 0; i < string.length(); i++) {
        char c = string.charAt(i);
        switch (c) {
          case '\t':
            escaped.append("\\t");
            break;
          case '\b':
            escaped.append("\\b");
            break;
          case '\n':
            escaped.append("\\n");
            break;
          case '\r':
            escaped.append("\\r");
            break;
          case '\f':
            escaped.append("\\f");
            break;
            // case '\'': escaped.append("\\'"); break; // not needed
          case '\"':
            escaped.append("\\\"");
            break;
          case '\\':
            escaped.append("\\\\");
            break;
          default:
            escaped.append(c);
        }
      }
      return new Argument(List.of(), '"' + escaped.toString() + '"');
    }

    static Argument of(Path path) {
      var unix = path.toString().replace("\\", "/");
      return new Argument(List.of("java.nio.file.Path"), "Path.of(\"" + unix + "\")");
    }

    static Argument of(String stringModifier, Path... paths) {
      var expressions = new ArrayList<String>();
      for (var path : paths) {
        var unix = path.toString().replace("\\", "/");
        expressions.add("Path.of(\"" + unix + "\")");
      }
      var expression = String.join(" + File.pathSeparator + ", expressions);
      if (paths.length == 1) {
        expression = expression + ".toString()" + stringModifier;
      } else {
        expression = '(' + expression + ')' + stringModifier;
      }
      return new Argument(List.of("java.io.File", "java.nio.file.Path"), expression);
    }

    final List<String> imports;
    final String expression;

    Argument(List<String> imports, String expression) {
      this.imports = imports;
      this.expression = expression;
    }

    @Override
    public String toString() {
      return expression;
    }
  }

  static class Action {

    final String method;
    final String name;
    final List<Argument> arguments;

    Action(String method, String name, List<Argument> arguments) {
      this.method = method;
      this.name = name;
      this.arguments = arguments;
    }

    String toJavaExpression() {
      if (arguments.isEmpty()) {
        return method + "(\"" + name + "\")";
      }
      var list = arguments.stream().map(Argument::toString).collect(Collectors.toList());
      var args = String.join(", ", list);
      return String.format("%s(\"%s\", %s)", method, name, args);
    }
  }

  static class Writer {

    final Path file;

    Writer(Path file) {
      this.file = file;
    }

    public void accept(List<Action> actions) {
      var imports =
          actions.stream()
              .flatMap(action -> action.arguments.stream())
              .flatMap(argument -> argument.imports.stream())
              .collect(Collectors.toCollection(TreeSet::new));
      var lines = new ArrayList<String>();
      lines.add("// Generated by Toccata " + VERSION + " at " + Instant.now());
      lines.add("");
      for (var type : imports) {
        lines.add(String.format("import %s;", type));
      }
      lines.add("");
      lines.add("class Build {");
      lines.add("  public static void main(String... args) {");
      actions.forEach(action -> lines.add(String.format("    %s;", action.toJavaExpression())));
      lines.add("  }");
      lines.add("");
      lines.add(
          "  static void run(String name, Object... args) {\n"
              + "   var line = name + (args.length == 0 ? \"\" : \" \" + String.join(\" \", strings(args)));\n"
              + "   System.out.println(\"| \" + line);\n"
              + "   var tool = java.util.spi.ToolProvider.findFirst(name).orElseThrow();\n"
              + "   var code = tool.run(System.out, System.err, strings(args));\n"
              + "   if (code != 0) throw new Error(code + \" <- \" + line);\n"
              + " }");
      lines.add("");
      lines.add(
          "  static String[] strings(Object... objects) {\n"
              + "    var strings = new java.util.ArrayList<String>();\n"
              + "    for (int i = 0; i < objects.length; i++) strings.add(objects[i].toString());\n"
              + "    return strings.toArray(String[]::new);\n"
              + "  }");
      lines.add("}");
      try {
        file.toFile().setWritable(true);
        Files.write(file, lines);
        file.toFile().setWritable(false);
      } catch (Exception e) {
        throw new Error("Writing actions failed: " + actions, e);
      }
    }

    static void run(String name, Object... args) {
      var line = name + (args.length == 0 ? "" : " " + String.join(" ", strings(args)));
      System.out.println("| " + line);
      var tool = java.util.spi.ToolProvider.findFirst(name).orElseThrow();
      var code = tool.run(System.out, System.err, strings(args));
      if (code != 0) throw new Error(code + " <- " + line);
    }

    static void start(Object... command) throws Exception {
      var line = String.join(" ", strings(command));
      System.out.println("| " + line);
      var process = new ProcessBuilder(strings(command)).inheritIO().start();
      var code = process.waitFor();
      if (code != 0) throw new Error(code + " <- " + line);
    }

    static String[] strings(Object... objects) {
      var strings = new java.util.ArrayList<String>();
      for (int i = 0; i < objects.length; i++) strings.add(objects[i].toString());
      return strings.toArray(String[]::new);
    }
  }
}
