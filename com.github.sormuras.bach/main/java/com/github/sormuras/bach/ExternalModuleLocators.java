package com.github.sormuras.bach;

import java.util.List;

public record ExternalModuleLocators(List<ExternalModuleLocator> list) {
  public static ExternalModuleLocators of(ExternalModuleLocator... locators) {
    return new ExternalModuleLocators(List.of(locators));
  }
}
