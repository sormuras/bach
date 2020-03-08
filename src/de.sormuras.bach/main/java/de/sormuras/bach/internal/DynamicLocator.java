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

import de.sormuras.bach.api.Locator;
import de.sormuras.bach.api.Maven;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

/** Compute a modular JAR URI based on educated guesses. */
public /*static*/ class DynamicLocator implements Locator {

  private final String repository;
  private final Map<String, String> variants;

  public DynamicLocator(String repository, Map<String, String> variants) {
    this.repository = repository;
    this.variants = variants;
  }

  @Override
  public Optional<URI> locate(String module) {
    var group = computeGroup(module);
    if (group == null) return Optional.empty();
    var artifact = computeArtifact(module, group);
    if (artifact == null) return Optional.empty();
    var version = variants.getOrDefault(module, computeVersion(module, group, artifact));
    if (version == null) return Optional.empty();
    var resource =
        Maven.newResource()
            .repository(repository)
            .group(group)
            .artifact(artifact)
            .version(version)
            .classifier(computeClassifier(module, group, artifact, version));
    return Optional.of(resource.build().get());
  }

  String computeGroup(String module) {
    if (module.startsWith("org.junit.platform")) return "org.junit.platform";
    if (module.startsWith("org.junit.jupiter")) return "org.junit.jupiter";
    if (module.startsWith("org.junit.vintage")) return "org.junit.vintage";
    switch (module) {
      case "junit":
        return "junit";
    }
    return null;
  }

  String computeArtifact(String module, String group) {
    // Group ID pattern: "org.junit.[jupiter|platform|vintage|.*]"
    if (group.startsWith("org.junit.")) return module.substring(4).replace('.', '-');
    switch (module) {
      case "junit":
        return "junit";
    }
    return null;
  }

  String computeVersion(String module, String group, String artifact) {
    switch (group) {
      case "junit":
        return "4.13";
      case "org.junit.jupiter":
      case "org.junit.vintage":
        return "5.6.0";
      case "org.junit.platform":
        return "1.6.0";
    }
    return null;
  }

  String computeClassifier(String module, String group, String artifact, String version) {
    return "";
  }
}
