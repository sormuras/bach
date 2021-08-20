package com.github.sormuras.bach;

import java.util.List;

public record ModuleLocators(List<ModuleLocator> list) {
  public static ModuleLocators of(ModuleLocator... locators) {
    return new ModuleLocators(List.of(locators));
  }
}
