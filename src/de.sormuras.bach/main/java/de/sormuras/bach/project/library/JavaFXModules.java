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

package de.sormuras.bach.project.library;

import de.sormuras.bach.project.Locator;
import java.util.Locale;

/**
 * OpenJFX is an open source, next generation client application platform.
 *
 * @see <a href="https://openjfx.io">openjfx.io</a>
 */
public /*static*/ class JavaFXModules extends Locator.AbstractLocator {

  public JavaFXModules() {
    putJavaFX("14.0.1", "base", "controls", "fxml", "graphics", "media", "swing", "web");
  }

  private void putJavaFX(String version, String... names) {
    var group = "org.openjfx";
    var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    var win = os.contains("win");
    var mac = os.contains("mac");
    var classifier = win ? "win" : mac ? "mac" : "linux";
    for (var name : names) {
      var module = "javafx." + name;
      var artifact = "javafx-" + name;
      var gav = String.join(":", group, artifact, version, classifier);
      var link = Maven.central(gav, module, 0, null);
      put(module, link);
    }
  }
}
