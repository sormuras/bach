/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import java.io.*;
import java.lang.module.ModuleFinder;
import java.nio.file.*;
import java.util.*;

/** Project build support. */
class Project {

  /** Source compilation fixture. */
  class Fixture {
    final String name;
    final Path destination;
    final List<Path> modulePath;
    final List<Path> moduleSourcePath;
    final Map<String, List<Path>> patchModule;

    Fixture(String name, Fixture... fixtures) {
      this.name = name;
      this.destination = target.resolve("bin/" + name + "/mods");
      this.modulePath = buildModulePath(List.of(Bach.Default.RESOLVE_PATH), fixtures);
      this.moduleSourcePath = List.of(Paths.get("src/" + name + "/java"));
      this.patchModule = buildPatchModule(moduleSourcePath, fixtures);
    }

    List<Path> buildModulePath(List<Path> dependencies, Fixture... fixtures) {
      var paths = new ArrayList<>(dependencies);
      for (Fixture fixture : fixtures) {
        paths.add(fixture.destination);
      }
      return Collections.unmodifiableList(paths);
    }

    Map<String, List<Path>> buildPatchModule(List<Path> basePaths, Fixture... fixtures) {
      var patchPaths = new ArrayList<Path>();
      for (Fixture fixture : fixtures) {
        patchPaths.addAll(fixture.moduleSourcePath);
      }
      return Bach.Basics.getPatchMap(basePaths, patchPaths);
    }
  }

  String name = Paths.get(".").toAbsolutePath().normalize().getFileName().toString();
  Path target = Paths.get("target", "bach");
  String version = "1.0.0-SNAPSHOT";

  Fixture mainFixture = new Fixture("main");
  Fixture testFixture = new Fixture("test", mainFixture);
  List<Fixture> fixtures = List.of(mainFixture, testFixture);

  void build() {
    Bach.log.level = Bach.Log.Level.VERBOSE;
    try {
      clean();
      resolve();
      compile();
      test();
      jar();
      // link();
    } catch (Exception e) {
      throw new AssertionError("build failed", e);
    }
  }

  void clean() throws IOException {
    Bach.Basics.treeDelete(target);
  }

  void resolve() throws IOException {}

  void compile() {
    fixtures.forEach(this::compile);
  }

  void compile(Fixture fixture) {
    Bach.log.info("Compile '" + fixture.name + "'");
    Bach.JdkTool.Javac javac = new Bach.JdkTool.Javac();
    javac.destination = fixture.destination;
    javac.modulePath = fixture.modulePath;
    javac.moduleSourcePath = fixture.moduleSourcePath;
    javac.patchModule = fixture.patchModule;
    javac.run();
  }

  void test() {
    var root = Paths.get(".").normalize().toAbsolutePath();
    var java = new Bach.JdkTool.Java();
    java.modulePath = List.of(testFixture.destination, Bach.Default.RESOLVE_PATH);
    java.module = "org.junit.platform.console";
    var command = java.toCommand();
    // TODO Use "--scan-module-path" when https://github.com/junit-team/junit5/pull/1061 is merged
    command.add("--scan-class-path");
    ModuleFinder.of(testFixture.destination)
        .findAll()
        .forEach(mr -> command.add("--class-path").add(root.relativize(Bach.Basics.getPath(mr))));
    command.run();
  }

  void jar() {
    jar(mainFixture);
  }

  void jar(Fixture fixture) {
    Bach.JdkTool.Jar jar = new Bach.JdkTool.Jar();
    jar.verbose = true;
    jar.file = Paths.get(fixture.name + ".jar");
    jar.path = fixture.destination;
    jar.run();

    Bach.run("jar", "--describe-module", "--file", jar.file);
  }
}
