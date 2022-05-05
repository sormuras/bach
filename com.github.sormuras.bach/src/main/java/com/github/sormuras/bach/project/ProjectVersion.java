package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor;
import java.time.ZonedDateTime;

public record ProjectVersion(String value, ZonedDateTime date) implements Project.Component {
  public ProjectVersion {
    ModuleDescriptor.Version.parse(value);
  }

  public ProjectVersion with(String value) {
    return new ProjectVersion(value, date);
  }

  public ProjectVersion with(ZonedDateTime date) {
    return new ProjectVersion(value, date);
  }

  public ProjectVersion withDate(String text) {
    return with(ZonedDateTime.parse(text));
  }
}
