package run.duke;

import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;
import run.duke.CommandLineInterface.Splitter;
import run.duke.tool.DukeTool;

public sealed interface Duke permits DukeTool {
  static <R extends Record> R split(Lookup lookup, Class<R> schema, String... args) {
    return splitter(lookup, schema).split(args);
  }

  static <R extends Record> Splitter<R> splitter(Lookup lookup, Class<R> schema) {
    return Splitter.of(lookup, schema)
        .withRemoveQuotes()
        .withSplitAssignment()
        .withExpand(Duke::expandArgumentsFileToArguments);
  }

  static ToolCall listTools() {
    return ToolCall.of("duke", "list", "tools");
  }

  static ToolCall treeDelete(Path start) {
    return ToolCall.of("duke", "tree", "--mode=DELETE", start);
  }

  static Stream<String> expandArgumentsFileToArguments(String argument) {
    if (!argument.startsWith("@")) return Stream.of(argument); // "abc" -> ["abc"]
    var substring = argument.substring(1); // "@...@abc" -> "...@abc"
    if (substring.startsWith("@")) return Stream.of(substring); // "@@abc" -> ["@abc"]
    var file = Path.of(substring);
    try {
      var expanded = new ArrayList<String>();
      for (var line : Files.readAllLines(file)) {
        line = line.strip();
        if (line.isEmpty()) continue; // skip empty line
        if (line.startsWith("#")) continue; // skip comment line
        expanded.add(line);
      }
      return expanded.stream();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
