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
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Map-based locator implementation. */
public /*static*/ class DirectLocator implements Locator {
  private final Map<String, URL> uris;

  public DirectLocator(Map<String, URL> urls) {
    this.uris = Objects.requireNonNull(urls, "urls");
  }

  @Override
  public Optional<URL> locate(String module) {
    return Optional.ofNullable(uris.get(module));
  }
}
