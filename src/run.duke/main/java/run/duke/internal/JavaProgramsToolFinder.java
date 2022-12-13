package run.duke.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;

public record JavaProgramsToolFinder(String description, Path path, Path java)
    implements ToolFinder {
  @Override
  public List<Tool> findTools() {
    return PathSupport.list(path, Files::isDirectory).stream()
        .map(directory -> new JavaProgramToolFinder(directory, java))
        .flatMap(finder -> finder.findTools().stream())
        .toList();
  }
}
