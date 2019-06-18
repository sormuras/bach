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

import static java.lang.System.Logger.Level.DEBUG;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/*BODY*/
/** Build, i.e. compile and package, a modular Java project. */
public /*STATIC*/ class JigsawBuilder {

  final Bach bach;
  final Path bin;
  final Path lib;
  final Path src;
  final String version;

  JigsawBuilder(Bach bach) {
    this.bach = bach;
    this.version = bach.project.version;
    this.bin = bach.run.work.resolve(bach.project.path(Project.Property.PATH_BIN));
    this.lib = bach.run.home.resolve(bach.project.path(Project.Property.PATH_LIB));
    this.src = bach.run.home.resolve(bach.project.path(Project.Property.PATH_SRC));
  }

   void build() throws Exception {
    compile("main");
    compile("test", "main");
    bach.run.log(DEBUG, "Build successful.");
  }

  void compile(String realm, String... requiredRealms) throws Exception {
    var modules = bach.project.modules(realm);
    compile(realm, modules, requiredRealms);
  }

  void compile(String realm, List<String> modules, String... requiredRealms) throws Exception {
    bach.run.log(DEBUG, "Compiling %s modules: %s", realm, modules);

    var classes = bin.resolve(realm + "/classes");
    var jars = Files.createDirectories(bin.resolve(realm + "/modules")); // "own" jars

    var modulePath = new ArrayList<>(bach.project.modulePath(realm, "compile", requiredRealms));
    modulePath.add(jars);
    for (var requiredRealm : requiredRealms) {
      modulePath.add(bin.resolve(requiredRealm + "/modules"));
    }

    bach.run.run(
        new Command("javac")
            .add("-d", classes)
            .add("--module-path", modulePath)
            .add("--module-source-path", src + "/*/" + realm + "/java")
            .add("--module-version", version)
            .add("--module", String.join(",", modules)));

    for (var module : modules) {
      var resources = src.resolve(Path.of(module, realm, "resources"));
      bach.run.run(
          new Command("jar")
              .add("--create")
              .addIff(bach.run.debug, "--verbose")
              .add("--file", jars.resolve(module + '-' + version + ".jar"))
              .add("-C", classes.resolve(module))
              .add(".")
              .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add(".")));
    }
  }
}
