package com.github.sormuras.bach.external;

import com.github.sormuras.bach.ExternalModuleLocator;

public record SingleExternalModuleLocator(String module, String uri)
    implements ExternalModuleLocator {
  @Override
  public String caption() {
    return "SingleModuleLocator: " + module + " -> " + uri;
  }

  @Override
  public String locate(String module) {
    return module().equals(module) ? uri : null;
  }
}
