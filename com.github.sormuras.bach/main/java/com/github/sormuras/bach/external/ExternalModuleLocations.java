package com.github.sormuras.bach.external;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ExternalModuleLocations(Map<String, ExternalModuleLocation> locations)
    implements ExternalModuleLocator {

  public static ExternalModuleLocations of(ExternalModuleLocation... locations) {
    return new ExternalModuleLocations(
        Stream.of(locations)
            .collect(Collectors.toMap(ExternalModuleLocation::module, Function.identity())));
  }

  @Override
  public Optional<ExternalModuleLocation> locate(String module) {
    return Optional.ofNullable(locations.get(module));
  }

  @Override
  public Stability stability() {
    return Stability.STABLE;
  }
}
