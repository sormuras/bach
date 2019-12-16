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

import java.util.Locale;
import java.util.Map;

/** String-based template constants and evaluation support. */
public class Template {

  /** Well-known placeholders. */
  public enum Placeholder {
    /** Example: {@code "${JAVAFX-PLATFORM}"} replaced by one of {@code "linux"|"mac"|"win"}. */
    JAVAFX_PLATFORM {
      @Override
      public String getDefault() {
        var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        return os.contains("win") ? "win" : os.contains("mac") ? "mac" : "linux";
      }
    },

    /** Example: {@code "${LWJGL-NATIVES}"} replaced by one of {@code "linux"|"macos"|"windows"}. */
    LWJGL_NATIVES {
      @Override
      public String getDefault() {
        var os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        // TODO missing lwjgl natives: linux-arm32, linux-arm64, windows-x86
        var natives = os.contains("win") ? "windows" : os.contains("mac") ? "macos" : "linux";
        return "natives-" + natives;
      }
    },

    /** Example: {@code "${GROUP}"} replaced by {@code "de.sormuras"}. */
    GROUP,
    /** Example: {@code "${MODULE}"} replaced by {@code "de.sormuras.bach"}. */
    MODULE,
    /** Example: {@code "${VERSION}"} replaced by {@code "1.2.3"}. */
    VERSION;

    private final String target;

    Placeholder() {
      this.target = "${" + name().replace('_', '-') + "}";
    }

    /** String to be replaced, like: {@code VERSION}. */
    public String getTarget() {
      return target;
    }

    /** Compute default value from runtime information. */
    public String getDefault() {
      throw new UnsupportedOperationException(name() + " has no default replacement");
    }
  }

  public static String replace(String template, Map<Placeholder, String> replacements) {
    if (template.indexOf('$') < 0) return template;
    var replaced = template;
    for (var entry : replacements.entrySet()) {
      var target = entry.getKey().getTarget();
      var replacement = entry.getValue();
      replaced = replaced.replace(target, replacement);
    }
    return replaced;
  }

  private Template() {
    throw new UnsupportedOperationException();
  }
}
