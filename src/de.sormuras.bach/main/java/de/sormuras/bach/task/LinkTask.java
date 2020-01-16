package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Unit;
import de.sormuras.bach.util.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class LinkTask implements Task {

  @Override
  public void execute(Bach bach) {
    var project = bach.getProject();
    var units = project.structure().units();
    var linkable = units.stream().filter(Unit::isLinkable).findFirst().orElse(null);
    if (linkable == null) {
      bach.getLog().debug("No linkable module unit found in: %s", units);
      return;
    }
    var realm = linkable.realm();
    var target = Paths.deleteIfExists(project.folder().realm(realm.name(), "image"));
    var modules = project.units(realm).stream().map(Unit::name);
    var modulePath = new ArrayList<Path>();
    modulePath.add(project.folder().modules(realm.name()));
    modulePath.addAll(realm.modulePaths());
    var jlink =
        new Call("jlink")
            .add("--output", target)
            .add("--add-modules", modules.collect(Collectors.joining(",")))
            .add("--launcher", project.name() + '=' + linkable.name())
            .add("--module-path", modulePath)
            .add("--compress", "2")
            .add("--no-header-files")
            .iff(bach.isVerbose(), c -> c.add("--verbose"));
    bach.execute(jlink);
  }
}
