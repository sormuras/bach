package run.bach.toolfinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import run.bach.Tool;
import run.bach.ToolFinder;
import run.bach.internal.PathSupport;

public record JavaProgramToolFinder(Path path, Path java) implements ToolFinder {
  @Override
  public List<Tool> findAll() {
    return findFirst(PathSupport.name(path, "?")).stream().toList();
  }

  @Override
  public Optional<Tool> findFirst(String name) {
    var directory = path.normalize().toAbsolutePath();
    if (!Files.isDirectory(directory)) return Optional.empty();
    var namespace = path.getParent().getFileName().toString();
    if (!name.equals(directory.getFileName().toString())) return Optional.empty();
    var command = new ArrayList<String>();
    command.add(java.toString());
    var args = directory.resolve("java.args");
    if (Files.isRegularFile(args)) {
      command.add("@" + args);
      return Optional.of(Tool.ofNativeProcess(namespace + '/' + name, command));
    }
    var jars = PathSupport.list(directory, PathSupport::isJarFile);
    if (jars.size() == 1) {
      command.add("-jar");
      command.add(jars.get(0).toString());
      return Optional.of(Tool.ofNativeProcess(namespace + '/' + name, command));
    }
    var javas = PathSupport.list(directory, PathSupport::isJavaFile);
    if (javas.size() == 1) {
      command.add(javas.get(0).toString());
      return Optional.of(Tool.ofNativeProcess(namespace + '/' + name, command));
    }
    return Optional.empty();
  }
}
