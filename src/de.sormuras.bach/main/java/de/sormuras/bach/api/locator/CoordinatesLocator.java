/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.api.locator;

import de.sormuras.bach.api.Locator;
import de.sormuras.bach.api.Maven;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Maven repository based locator implementation parsing complete coordinates. */
public /*static*/ class CoordinatesLocator implements Locator {

  private final String repository;
  private final Map<String, String> coordinates;

  public CoordinatesLocator(String repository, Map<String, String> coordinates) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.coordinates = Objects.requireNonNull(coordinates, "coordinates");
  }

  @Override
  public Optional<Location> locate(String module) {
    var coordinate = coordinates.get(module);
    if (coordinate == null) return Optional.empty();
    var split = coordinate.split(":");
    if (split.length < 3) throw new RuntimeException("Expected Maven GAV, but got: " + coordinate);
    var group = split[0];
    var artifact = split[1];
    var version = split[2];
    var resource = Maven.newResource().repository(repository);
    resource.group(group).artifact(artifact).version(version);
    resource.classifier(split.length < 4 ? "" : split[3]);
    var uri = resource.build().get();
    return Optional.of(new Location(uri, version));
  }
}
