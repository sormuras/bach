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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/*BODY*/
/** Build, i.e. compile and package, a modular Java project. */
public /*STATIC*/ class JigsawBuilder implements Action {

  static List<String> modules(Project project, String realm) {
    var userDefinedModules = project.get(Project.Property.MODULES);
    if (!userDefinedModules.equals("*")) {
      return List.of(userDefinedModules.split("\\s*,\\s*"));
    }
    // Find modules for realm...
    var modules = new ArrayList<String>();
    var descriptor = Path.of(realm, "java", "module-info.java");
    DirectoryStream.Filter<Path> filter =
        path -> Files.isDirectory(path) && Files.exists(path.resolve(descriptor));
    try (var stream = Files.newDirectoryStream(project.path(Project.Property.PATH_SRC), filter)) {
      stream.forEach(directory -> modules.add(directory.getFileName().toString()));
    } catch (Exception e) {
      throw new Error(e);
    }
    return modules;
  }

  @Override
  public void perform(Bach bach) throws Exception {
    var worker = new Worker(bach);
    worker.compile("main");
  }

  static class Worker {
    final Bach bach;
    final Path bin;
    final Path lib;
    final Path src;
    final String version;

    Worker(Bach bach) {
      this.bach = bach;
      this.version = bach.project.version;
      this.bin = bach.run.work.resolve(bach.project.path(Project.Property.PATH_BIN));
      this.lib = bach.run.home.resolve(bach.project.path(Project.Property.PATH_LIB));
      this.src = bach.run.home.resolve(bach.project.path(Project.Property.PATH_SRC));
    }

    void compile(String realm) throws Exception {
      var modules = modules(bach.project, realm);
      compile(realm, modules);
    }

    void compile(String realm, List<String> modules) throws Exception {
      bach.run.log(DEBUG, "Compiling %s modules: %s", realm, modules);
      var classes = bin.resolve(realm + "/classes");

      bach.run.run(
          new Command("javac")
              .add("-d", classes)
              .add("--module-source-path", src + "/*/" + realm + "/java")
              .add("--module-version", version)
              .add("--module", String.join(",", modules)));

      var jars = Files.createDirectories(bin.resolve(realm + "/modules"));
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
}
