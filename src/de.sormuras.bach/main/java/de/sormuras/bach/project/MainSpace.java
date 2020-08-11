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

package de.sormuras.bach.project;

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Modules;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** A code space for {@code main} modules. */
public final class MainSpace implements CodeSpace<MainSpace> {

  private final Set<Feature> features;
  private final JavaRelease release;
  private final CodeUnits units;

  public MainSpace(Set<Feature> features, JavaRelease release, CodeUnits units) {
    this.features = features.isEmpty() ? Set.of() : EnumSet.copyOf(features);
    this.release = release;
    this.units = units;
  }

  public Set<Feature> features() {
    return features;
  }

  public JavaRelease release() {
    return release;
  }

  public CodeUnits units() {
    return units;
  }

  @Factory
  public static MainSpace of() {
    return new MainSpace(Feature.DEFAULTS, JavaRelease.ofRuntime(), CodeUnits.of());
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace features(Set<Feature> features) {
    return new MainSpace(features, release, units);
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace release(JavaRelease release) {
    return new MainSpace(features, release, units);
  }

  @Factory(Factory.Kind.SETTER)
  public MainSpace units(CodeUnits units) {
    return new MainSpace(features, release, units);
  }

  @Factory(Factory.Kind.OPERATOR)
  public MainSpace with(Feature... moreFeatures) {
    var mergedFeatures = new TreeSet<>(features);
    mergedFeatures.addAll(Set.of(moreFeatures));
    return features(mergedFeatures);
  }

  @Factory(Factory.Kind.OPERATOR)
  public MainSpace without(Feature... redundantFeatures) {
    var mergedFeatures = new TreeSet<>(features);
    mergedFeatures.removeAll(Set.of(redundantFeatures));
    return features(mergedFeatures);
  }

  @Override
  public String name() {
    return "";
  }

  public boolean is(Feature feature) {
    return features.contains(feature);
  }

  public Optional<String> findMainModule() {
    return Modules.findMainModule(units.toUnits().map(CodeUnit::descriptor));
  }
}
