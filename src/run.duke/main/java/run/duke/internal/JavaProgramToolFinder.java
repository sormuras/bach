package run.duke.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import run.duke.Tool;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record JavaProgramToolFinder(Path path, Path java) implements ToolFinder {
  @Override
  public List<String> identifiers() {
    var directory = path.normalize().toAbsolutePath();
    if (!Files.isDirectory(directory)) return List.of();
    var namespace = directory.getParent().getFileName().toString();
    var nickname = directory.getFileName().toString();
    var identifier = namespace + '/' + nickname;
    return List.of(identifier);
  }

  @Override
  public Optional<Tool> find(String string, ToolRunner runner) {
    var directory = path.normalize().toAbsolutePath();
    if (!Files.isDirectory(directory)) return Optional.empty();
    var namespace = directory.getParent().getFileName().toString();
    var nickname = directory.getFileName().toString();
    var identifier = namespace + '/' + nickname;
    if (!(string.equals(nickname) || string.equals(identifier))) return Optional.empty();
    var command = new ArrayList<String>();
    command.add(java.toString());
    var args = directory.resolve("java.args");
    if (Files.isRegularFile(args)) {
      command.add("@" + args);
      var provider = new NativeProcessToolProvider(string, command);
      return Optional.of(new Tool(namespace, string, provider));
    }
    var jars = PathSupport.list(directory, PathSupport::isJarFile);
    if (jars.size() == 1) {
      command.add("-jar");
      command.add(jars.get(0).toString());
      var provider = new NativeProcessToolProvider(string, command);
      return Optional.of(new Tool(namespace, string, provider));
    }
    var javas = PathSupport.list(directory, PathSupport::isJavaFile);
    if (javas.size() == 1) {
      command.add(javas.get(0).toString());
      var provider = new NativeProcessToolProvider(string, command);
      return Optional.of(new Tool(namespace, string, provider));
    }
    return Optional.empty();
  }
}
