package run.duke.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import run.duke.Tool;
import run.duke.Toolbox;

public record JavaProgramToolbox(Path path, Path java) implements Toolbox {
  @Override
  public List<Tool> tools() {
    var directory = path.normalize().toAbsolutePath();
    if (!Files.isDirectory(directory)) return List.of();
    var nickname = directory.getFileName().toString();
    return findTool(nickname).stream().toList();
  }

  @Override
  public Optional<Tool> findTool(String string) {
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
      return Optional.of(Tool.of(identifier, provider));
    }
    var jars = PathSupport.list(directory, PathSupport::isJarFile);
    if (jars.size() == 1) {
      command.add("-jar");
      command.add(jars.get(0).toString());
      var provider = new NativeProcessToolProvider(string, command);
      return Optional.of(Tool.of(identifier, provider));
    }
    var javas = PathSupport.list(directory, PathSupport::isJavaFile);
    if (javas.size() == 1) {
      command.add(javas.get(0).toString());
      var provider = new NativeProcessToolProvider(string, command);
      return Optional.of(Tool.of(identifier, provider));
    }
    return Optional.empty();
  }
}
