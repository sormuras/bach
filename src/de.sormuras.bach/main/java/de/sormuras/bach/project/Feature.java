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

import java.util.Set;

/** A feature flag on a main code space. */
public enum Feature {
  /** Generate HTML pages of API documentation from main source files. */
  CREATE_API_DOCUMENTATION,

  /** Assemble and optimize main modules and their dependencies into a custom runtime image. */
  CREATE_CUSTOM_RUNTIME_IMAGE,

  /** Include {@code *.java} files alongside with {@code *.class} files into each modular JAR. */
  INCLUDE_SOURCES_IN_MODULAR_JAR,

  /** Include all resource files alongside with {@code *.java} files into each sources JAR. */
  INCLUDE_RESOURCES_IN_SOURCES_JAR;

  /** The default feature set on a main code space. */
  public static final Set<Feature> DEFAULTS =
      Set.of(CREATE_API_DOCUMENTATION, CREATE_CUSTOM_RUNTIME_IMAGE);
}
