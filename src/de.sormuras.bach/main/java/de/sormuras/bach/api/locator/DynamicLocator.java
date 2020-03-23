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
import java.util.Locale;
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
  public Optional<Location> locate(String module) {
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
    var uri = resource.build().get();
    return Optional.of(new Location(uri, version));
  }

  String computeGroup(String module) {
    if (module.startsWith("javafx.")) return "org.openjfx";
    if (module.startsWith("org.junit.platform")) return "org.junit.platform";
    if (module.startsWith("org.junit.jupiter")) return "org.junit.jupiter";
    if (module.startsWith("org.junit.vintage")) return "org.junit.vintage";
    switch (module) {
      case "org.apiguardian.api":
        return "org.apiguardian";
      case "org.opentest4j":
        return "org.opentest4j";
      case "junit":
        return "junit";
    }
    return null;
  }

  String computeArtifact(String module, String group) {
    // JUnit Group ID pattern: "org.junit.[jupiter|platform|vintage|.*]"
    if (group.startsWith("org.junit.")) return module.substring(4).replace('.', '-');
    switch (module) {
      case "org.apiguardian.api":
        return "apiguardian-api";
      case "org.opentest4j":
        return "opentest4j";
      case "junit":
        return "junit";
    }
    switch (group) {
      case "org.openjfx":
        return "javafx-" + module.substring(7);
    }
    return null;
  }

  String computeVersion(String module, String group, String artifact) {
    switch (module) {
      case "org.apiguardian.api":
        return "1.1.0";
      case "org.opentest4j":
        return "1.2.0";
      case "junit":
        return "4.13";
    }
    switch (group) {
      case "org.openjfx":
        return "14";
      case "junit":
        return "4.13";
      case "org.junit.jupiter":
      case "org.junit.vintage":
        return "5.6.1";
      case "org.junit.platform":
        return "1.6.1";
    }
    return null;
  }

  String computeClassifier(String module, String group, String artifact, String version) {
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var win = os.contains("win");
    var mac = os.contains("mac");
    if (group.equals("org.openjfx")) return win ? "win" : mac ? "mac" : "linux";
    return "";
  }
}
