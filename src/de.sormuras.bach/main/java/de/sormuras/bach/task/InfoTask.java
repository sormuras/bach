package de.sormuras.bach.task;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Task;

public class InfoTask implements Task {

  @Override
  public void execute(Bach bach) {
    var log = bach.getLog();
    var project = bach.getProject();
    log.info("Project %s %s", project.name(), project.version());
    try {
      for (var field : project.getClass().getFields()) {
        log.debug("  %s = %s", field.getName(), field.get(project));
      }
      for (var realm : project.structure().realms()) {
        log.debug("+ Realm %s", realm.name());
        for (var method : realm.getClass().getDeclaredMethods()) {
          if (method.getParameterCount() != 0) continue;
          log.debug("  %s.%s() = %s", realm.name(), method.getName(), method.invoke(realm));
        }
        for (var unit : project.structure().units()) {
          log.debug("- Unit %s", unit.name());
          for (var method : unit.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() != 0) continue;
            log.debug("  (%s).%s() = %s", unit.name(), method.getName(), method.invoke(unit));
          }
        }
      }
    } catch (ReflectiveOperationException e) {
      log.warning(e.getMessage());
    }
  }
}
