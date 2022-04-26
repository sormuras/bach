package com.github.sormuras.bach.project;

import java.lang.module.ModuleDescriptor;
import java.time.ZonedDateTime;

public record Version(String value, ZonedDateTime date) implements Project.Component {
  public Version {
    ModuleDescriptor.Version.parse(value);
  }

  public Version with(String value) {
    return new Version(value, date);
  }

  public Version with(ZonedDateTime date) {
    return new Version(value, date);
  }

  public Version withDate(String text) {
    return with(ZonedDateTime.parse(text));
  }
}
