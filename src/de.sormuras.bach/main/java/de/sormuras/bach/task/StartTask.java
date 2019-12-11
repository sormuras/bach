package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Unit;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class StartTask implements Task {

  private final Iterable<String> arguments;

  public StartTask(Iterable<String> arguments) {
    this.arguments = arguments;
  }

  @Override
  public void execute(Bach bach) throws Exception {
    var project = bach.getProject();
    var units = project.structure().units();
    var mains = units.stream().filter(Unit::isMainClassPresent).collect(Collectors.toList());
    if (mains.isEmpty()) {
      bach.getLog().warning("No main class found.");
      return;
    }
    if (mains.size() > 1) {
      bach.getLog().warning("Multiple units with main-class found: %s", mains);
      return;
    }
    var unit = mains.get(0);
    var realm = unit.realm();
    var modulePath = new ArrayList<Path>();
    modulePath.add(project.folder().modules(realm.name()));
    modulePath.addAll(realm.modulePaths());

    var java = new Call(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    java.add("--module-path", modulePath);
    java.add("--module", unit.name());
    java.forEach(arguments, Call::add); // pass all remaining args
    var code = bach.start(java.toList(true));
    if (code != 0) throw new Error("Non-zero exit code: " + code + " // " + java);
  }
}
