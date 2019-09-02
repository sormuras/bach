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

package de.sormuras.bach;

import java.nio.file.Files;
import java.util.Collection;

/*BODY*/
/** Default multi-module compiler. */
/*STATIC*/ class Jigsaw {

  final Bach bach;

  Jigsaw(Bach bach) {
    this.bach = bach;
  }

  Collection<String> compile(Realm realm, Collection<String> modules) {
    var destination = realm.getDestination().resolve("compile/jigsaw");
    var javac =
        new Command("javac")
            .add("-d", destination)
            // .addEach(configuration.lines(Property.OPTIONS_JAVAC))
            // .addIff(realm.preview, "--enable-preview")
            // .addIff(realm.release != null, "--release", realm.release)
            .add("--module-path", realm.getModulePaths())
            .add("--module-source-path", realm.getModuleSourcePath())
            .add("--module", String.join(",", modules))
            .add("--module-version", realm.getVersion());
    realm.addModulePatches(javac, modules);
    bach.run(javac);

    // Util.treeCreate(realm.binSources);
    for (var module : modules) {
      var info = realm.getDeclaredModuleInfoMap().get(module);
      var modularJar = info.getModularJar();
      var resources = info.getResources();
      // var mainClass = realm.declaredModules.get(module).mainClass();
      Util.treeCreate(modularJar.getParent()); // jar doesn't create directories...
      var jarModule =
          new Command("jar")
              .add("--create")
              .add("--file", modularJar)
              // .addIff(configuration.verbose(), "--verbose")
              // .addIff("--main-class", mainClass)
              .add("-C", destination.resolve(module))
              .add(".")
              .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add("."));
      bach.run(jarModule);
      /*
       var sourcesJar = realm.sourcesJar(module);
       var jarSources =
           new Command("jar")
               .add("--create")
               .add("--file", sourcesJar)
               // .addIff(configuration.verbose(), "--verbose")
               .add("--no-manifest")
               // .add("-C", project.sources.resolve(module).resolve(realm.name).resolve("java"))
               .add(".")
               .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add("."));
       // if (runner.run(jarSources) != 0) {
       //  throw new RuntimeException(
       //      "Creating " + realm.name + " sources jar failed: " + sourcesJar);
       //}
      */
    }

    return modules;
  }
}
