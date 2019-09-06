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

// default package

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;

/** Custom configuration. */
public class Configuration extends Bach.Configuration {

  @Override
  public String getProjectName() {
    return "Bach.java";
  }

  @Override
  public Version getProjectVersion() {
    return Version.parse("2-ea");
  }

  @Override
  public List<Path> getSourceDirectories() {
    return List.of(getHomeDirectory().resolve("src/modules"));
  }

  @Override
  public String getModuleVersion(String module) {
    if ("de.sormuras.mainrunner.engine".equals(module)) return "2.0.5";
    if ("org.apiguardian.api".equals(module)) return "1.1.0";
    if ("org.opentest4j".equals(module)) return "1.2.0";
    if (module.startsWith("org.junit.jupiter")) return "5.5.1";
    if (module.startsWith("org.junit.platform")) return "1.5.1";
    return null;
  }

  @Override
  public String toString() {
    return "Bach.java eating its own dog food";
  }
}
