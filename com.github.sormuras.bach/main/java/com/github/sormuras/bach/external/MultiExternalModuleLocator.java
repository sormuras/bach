package com.github.sormuras.bach.external;

import com.github.sormuras.bach.ExternalModuleLocator;
import java.util.Map;

public record MultiExternalModuleLocator(Map<String, String> map) implements ExternalModuleLocator {
  @Override
  public String caption() {
    return "MultiModuleLocator for %d module%s".formatted(map.size(), map.size() == 1 ? "" : "s");
  }

  @Override
  public String locate(String module) {
    return map.get(module);
  }
}
