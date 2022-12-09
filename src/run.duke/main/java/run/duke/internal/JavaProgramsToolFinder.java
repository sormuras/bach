package run.duke.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record JavaProgramsToolFinder(String description, Path path, Path java)
    implements ToolFinder {
  @Override
  public Optional<Tool> find(String string, ToolRunner runner) {
    return PathSupport.list(path, Files::isDirectory).stream()
        .map(directory -> new JavaProgramToolFinder(directory, java))
        .flatMap(finder -> finder.find(string, runner).stream())
        .findFirst();
  }

  @Override
  public List<String> identifiers(ToolRunner runner) {
    return PathSupport.list(path, Files::isDirectory).stream()
        .map(directory -> new JavaProgramToolFinder(directory, java))
        .map(finder -> finder.identifiers(runner))
        .flatMap(List::stream)
        .toList();
  }
}
