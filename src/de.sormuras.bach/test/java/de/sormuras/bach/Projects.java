package de.sormuras.bach;

import java.lang.module.ModuleDescriptor.Version;
import java.util.List;

public interface Projects {
  static Project zero() {
    return new Project(
        Project.Base.of(),
        new Project.Info("Zero", Version.parse("0")),
        Project.Library.of(),
        List.of());
  }
}
