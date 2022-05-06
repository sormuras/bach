package com.github.sormuras.bach.project;

import java.util.Map;
import java.util.Optional;

/** An external module locator tries to link a module name to a remote location. */
@FunctionalInterface
public interface ExternalModuleLocator {

  String locate(String module);

  default Optional<String> find(String module) {
    return Optional.ofNullable(locate(module));
  }

  default String caption() {
    return getClass().getSimpleName();
  }

  record SingleExternalModuleLocator(String module, String uri) implements ExternalModuleLocator {
    @Override
    public String caption() {
      return "SingleExternalModuleLocator: " + module + " -> " + uri;
    }

    @Override
    public String locate(String module) {
      return module().equals(module) ? uri : null;
    }
  }

  record MultiExternalModuleLocator(Map<String, String> map) implements ExternalModuleLocator {
    @Override
    public String caption() {
      return "MultiExternalModuleLocator for %d module%s"
          .formatted(map.size(), map.size() == 1 ? "" : "s");
    }

    @Override
    public String locate(String module) {
      return map.get(module);
    }
  }
}
