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

import java.lang.module.ModuleDescriptor.Version;
import java.util.Locale;

/** String-based template constants and evaluation support. */
public enum Template {
  JAVAFX_PLATFORM,
  VERSION;

  private final String placeholder;

  Template() {
    this.placeholder = "${" + name().replace('_', '-') + "}";
  }

  public String getPlaceholder() {
    return placeholder;
  }

  public static String replace(String template, Version version) {
    if (template.indexOf('$') < 0) return template;
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var javafxPlatform = os.contains("win") ? "win" : os.contains("mac") ? "mac" : "linux";
    return template
        .replace(VERSION.placeholder, version.toString())
        .replace(JAVAFX_PLATFORM.placeholder, javafxPlatform);
  }
}
