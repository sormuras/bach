/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package de.sormuras.bach.project;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public /*record*/ class Unit {

  private final Realm realm;
  private final ModuleDescriptor descriptor;
  private final Path info;
  private final Path pom;
  private final List<Source> sources;
  private final List<Path> resources;
  private final List<Path> patches;

  public Unit(
      Realm realm,
      ModuleDescriptor descriptor,
      Path info,
      Path pom,
      List<Source> sources,
      List<Path> resources,
      List<Path> patches) {
    this.realm = Objects.requireNonNull(realm, "realm");
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    this.info = Objects.requireNonNull(info, "info");
    this.pom = pom;
    this.sources = List.copyOf(sources);
    this.resources = List.copyOf(resources);
    this.patches = List.copyOf(patches);
  }

  public Realm realm() {
    return realm;
  }

  public ModuleDescriptor descriptor() {
    return descriptor;
  }

  public String name() {
    return descriptor.name();
  }

  public Path info() {
    return info;
  }

  public Optional<Path> mavenPom() {
    return pom != null && Files.isRegularFile(pom) ? Optional.of(pom) : Optional.empty();
  }

  public List<Source> sources() {
    return sources;
  }

  public <T> List<T> sources(Function<Source, T> mapper) {
    return sources.stream().map(mapper).collect(Collectors.toList());
  }

  public List<Path> resources() {
    return resources;
  }

  public List<Path> patches() {
    return patches;
  }

  @Override
  public String toString() {
    return descriptor.toNameAndVersion();
  }

  public boolean isMultiRelease() {
    return !sources.isEmpty() && sources.stream().allMatch(Source::isTargeted);
  }

  public boolean isMainClassPresent() {
    return descriptor.mainClass().isPresent();
  }
}
