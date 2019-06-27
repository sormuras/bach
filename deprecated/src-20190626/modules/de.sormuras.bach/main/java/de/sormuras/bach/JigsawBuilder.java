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
import static java.lang.System.Logger.Level.TRACE;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/*BODY*/
/** Build, i.e. compile and package, a modular Java project. */
public /*STATIC*/ class JigsawBuilder implements Callable<Integer> {

  final Bach bach;

  JigsawBuilder(Bach bach) {
    this.bach = bach;
  }

  public Integer call() throws Exception {
    bach.run.log(TRACE, "JigsawBuilder::call()");
    compile("main");
    compile("test", "main");
    return 0;
  }

  void compile(String realm, String... requiredRealms) throws Exception {
    var modules = bach.project.modules(realm);
    if (modules.isEmpty()) {
      bach.run.log(DEBUG, "No %s modules found.", realm);
      return;
    }
    compile(realm, modules, requiredRealms);
    bach.run.log(DEBUG, "Jigsaw %s compilation successful.", realm);
  }

  void compile(String realm, List<String> modules, String... requiredRealms) throws Exception {
    bach.run.log(DEBUG, "Compiling %s modules: %s", realm, modules);

    var bin = bach.project.bin;
    var classes = bin.resolve(realm + "/classes");
    var binModules = Files.createDirectories(bin.resolve(realm + "/modules")); // "own" jars
    var binSources = Files.createDirectories(bin.resolve(realm + "/sources"));

    var modulePath = new ArrayList<>(bach.project.modulePath(realm, "compile", requiredRealms));
    modulePath.add(binModules);
    for (var requiredRealm : requiredRealms) {
      modulePath.add(bin.resolve(requiredRealm + "/modules"));
    }
    var src = bach.project.src;
    var moduleSourcePath = String.join(File.separator, src.toString(), "*", realm, "java");

    var version = bach.project.version;
    var preview = "enable".equals(bach.project.get(realm + ".javac.preview"));
    var defaultRelease = preview ? "" + Runtime.version().feature() : null;
    var release = bach.project.get(realm + ".javac.release", defaultRelease);
    bach.run.run(
        new Command("javac")
            .add("-d", classes)
            .addIff(preview,"--enable-preview")
            .addIff(release != null, "--release", release)
            .add("--module-path", modulePath)
            .add("--module-source-path", moduleSourcePath)
            .add("--module-version", version)
            .add("--module", String.join(",", modules)));

    for (var module : modules) {
      var resources = src.resolve(Path.of(module, realm, "resources"));
      bach.run.run(
          new Command("jar")
              .add("--create")
              .addIff(bach.run.debug, "--verbose")
              .add("--file", binModules.resolve(module + '-' + version + ".jar"))
              .add("-C", classes.resolve(module))
              .add(".")
              .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add(".")));
      bach.run.run(
          new Command("jar")
              .add("--create")
              .addIff(bach.run.debug, "--verbose")
              .add("--file", binSources.resolve(module + '-' + version + "-sources.jar"))
              .add("-C", src.resolve(Path.of(module, realm, "java")))
              .add(".")
              .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add(".")));
    }
  }
}
