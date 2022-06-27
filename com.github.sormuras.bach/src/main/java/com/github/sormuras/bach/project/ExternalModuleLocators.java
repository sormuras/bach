package com.github.sormuras.bach.project;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/** A sequence of external module locator instances. */
public record ExternalModuleLocators(List<ExternalModuleLocator> list)
    implements Iterable<ExternalModuleLocator> {

  public static ExternalModuleLocators of(ExternalModuleLocator... locators) {
    return new ExternalModuleLocators(List.of(locators));
  }

  @Override
  public Iterator<ExternalModuleLocator> iterator() {
    return list.iterator();
  }

  public String locate(String module) {
    for (var locator : list) {
      var location = locator.locate(module);
      if (location != null) return location;
    }
    return null;
  }

  public ExternalModuleLocators with(ExternalModuleLocator... locators) {
    var stream = Stream.concat(list.stream(), Stream.of(locators));
    return new ExternalModuleLocators(stream.toList());
  }
}
