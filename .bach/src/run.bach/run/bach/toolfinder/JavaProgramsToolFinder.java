package run.bach.toolfinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import run.bach.Tool;
import run.bach.ToolFinder;
import run.bach.internal.PathSupport;

public record JavaProgramsToolFinder(String description, Path path, Path java)
    implements ToolFinder {
  @Override
  public List<Tool> findAll() {
    return PathSupport.list(path, Files::isDirectory).stream()
        .map(directory -> new JavaProgramToolFinder(directory, java))
        .map(ToolFinder::findAll)
        .flatMap(List::stream)
        .toList();
  }
}
