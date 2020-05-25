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

package de.sormuras.bach.internal;

import de.sormuras.bach.Project;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

/** {@link Project.Locator}-related utilities. */
public /*static*/ class Locators {

  public static class ComposedLocator implements Project.Locator {

    private final Iterable<Project.Locator> locators;

    public ComposedLocator(Iterable<Project.Locator> locators) {
      this.locators = locators;
    }

    @Override
    public String apply(String module) {
      return Math.random() <= 0.5 ? withForEach(module) : withStream(module);
    }

    public String withForEach(String module) {
      for (var locator : locators) {
        var uri = locator.apply(module);
        if (uri != null) return uri;
      }
      return null;
    }

    public String withStream(String module) {
      var stream = StreamSupport.stream(locators.spliterator(), false);
      return stream
          .map(locator -> locator.apply(module))
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
    }
  }

  public static class MavenLocator implements Project.Locator {

    private final String repository;
    private final Map<String, String> coordinates;

    public MavenLocator(Map<String, String> coordinates) {
      this(Maven.CENTRAL_REPOSITORY, coordinates);
    }

    public MavenLocator(String repository, Map<String, String> coordinates) {
      this.repository = repository;
      this.coordinates = coordinates;
    }

    @Override
    public String apply(String module) {
      var coordinate = coordinates.get(module);
      if (coordinate == null) return null;
      var split = coordinate.split(":");
      if (split.length < 3) return coordinate;
      var group = split[0];
      var artifact = split[1];
      var version = split[2];
      var joiner = new Maven.Joiner().repository(repository);
      joiner.group(group).artifact(artifact).version(version);
      joiner.classifier(split.length < 4 ? "" : split[3]);
      return joiner.toString();
    }
  }

  private Locators() {}
}
