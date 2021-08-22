package com.github.sormuras.bach;

import java.util.List;

/** A list of external module locator instances that are queried in order. */
public record ExternalModuleLocators(List<ExternalModuleLocator> list) {
  public static ExternalModuleLocators of(ExternalModuleLocator... locators) {
    return new ExternalModuleLocators(List.of(locators));
  }
}
