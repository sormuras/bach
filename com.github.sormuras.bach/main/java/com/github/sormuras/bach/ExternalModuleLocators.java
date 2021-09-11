package com.github.sormuras.bach;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** A sequence of external module locator instances. */
public record ExternalModuleLocators(List<ExternalModuleLocator> values)
    implements Iterable<ExternalModuleLocator> {

  public static ExternalModuleLocators of(ExternalModuleLocator... locators) {
    return new ExternalModuleLocators(List.of(locators));
  }

  @Override
  public Iterator<ExternalModuleLocator> iterator() {
    return values().iterator();
  }

  public ExternalModuleLocators with(ExternalModuleLocator locator) {
    var values = new ArrayList<>(this.values);
    values.add(locator);
    return new ExternalModuleLocators(List.copyOf(values));
  }
}
