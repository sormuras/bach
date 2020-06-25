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

import de.sormuras.bach.internal.SormurasModulesProperties;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/** A module locator manager. */
public final class Locators {

  public static Locators of() {
    return new Locators(Map.of(), __ -> Optional.empty());
  }

  private final Map<String, Locator> fixed;
  private final Function<String, Optional<Locator>> dynamic;

  public Locators(Map<String, Locator> fixed, Function<String, Optional<Locator>> dynamic) {
    this.fixed = Map.copyOf(fixed);
    this.dynamic = dynamic;
  }

  public Map<String, Locator> fixed() {
    return fixed;
  }

  public Function<String, Optional<Locator>> dynamic() {
    return dynamic;
  }

  public Optional<Locator> findLocator(String module) {
    var direct = fixed().get(module);
    if (direct != null) return Optional.of(direct);
    return dynamic().apply(module);
  }

  public Locators with(Locator... locators) {
    var map = new TreeMap<>(fixed());
    for (var locator : locators) map.put(locator.module(), locator);
    return new Locators(map, dynamic);
  }

  public Locators with(Function<String, Optional<Locator>> dynamic) {
    return new Locators(fixed, dynamic);
  }

  public Locators withDynamicSormurasModulesLocatorFactory(Map<String, String> versions) {
    return with(new SormurasModulesLocatorFactory(versions));
  }

  static class SormurasModulesLocatorFactory implements Function<String, Optional<Locator>> {

    private final SormurasModulesProperties properties;

    public SormurasModulesLocatorFactory(Map<String, String> versions) {
      this.properties = new SormurasModulesProperties(versions);
    }

    @Override
    public Optional<Locator> apply(String module) {
      return properties.lookup(module).map(uri -> new Locator(module, uri));
    }
  }
}
