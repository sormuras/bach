package de.sormuras.bach;

import java.lang.module.ModuleDescriptor;
import java.util.List;

public interface Projects {
  static Project zero() {
    return new Project(
        Project.Base.of(),
        new Project.Info("Zero", ModuleDescriptor.Version.parse("0")),
        new Project.Structure(Project.Library.of(), List.of()));
  }
}
