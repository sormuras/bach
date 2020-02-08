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

// default package

import de.sormuras.bach.Bach;
import de.sormuras.bach.Log;
import de.sormuras.bach.project.Configuration;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;

/** Bach.java's build program, using module {@code de.sormuras.bach} itself. */
public class Build {

  private static final String VERSION = "2.1-ea";

  public static void main(String... args) {
    System.out.println("Building Bach.java " + VERSION + "...");
    Bach.build(
        Configuration.of("Bach.java", VERSION)
            .setLog(Log.ofSystem(true))
            .setMainRelease(11)
            .setModuleDescriber(Build::describeModule)
            // .setModuleLinker(Build::linkModule)
            .setGroup("de.sormuras.bach"));
  }

  static ModuleDescriptor describeModule(Path info) {
    var builder = Configuration.newModule(info);
    var module = builder.build();
    if (module.name().equals("de.sormuras.bach")) builder.mainClass(Bach.class.getName());
    return builder.build();
  }

  //  static Library.Link linkModule(String module) {
  //    switch (module) {
  //      case "junit":
  //        return Library.Link.central("junit", "junit", "4.13");
  //    }
  //    return Library.defaultLinks().get(module);
  //  }
}
