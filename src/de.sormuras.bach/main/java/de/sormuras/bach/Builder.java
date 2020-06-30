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

package de.sormuras.bach;

import de.sormuras.bach.internal.Modules;
import de.sormuras.bach.internal.Paths;
import de.sormuras.bach.project.Base;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.SourceDirectory;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.project.SourceUnits;
import de.sormuras.bach.tool.Jar;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.io.File;
import java.lang.System.Logger.Level;
import java.lang.module.FindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** An extensible build workflow. */
public class Builder {

  @FunctionalInterface
  public interface Factory {
    Builder create(Bach bach);
  }

  private final Bach bach;

  public Builder(Bach bach) {
    this.bach = bach;
  }

  public final Bach bach() {
    return bach;
  }

  public final Project project() {
    return bach.project();
  }

  public final Base base() {
    return bach.project().base();
  }

  public final MainSources main() {
    return bach.project().sources().main();
  }

  public void build() {
    // TODO downloadMissingExternalModules();
    if (main().units().isPresent()) {
      buildMainModules();
      var service = Executors.newWorkStealingPool();
      service.execute(this::buildApiDocumentation);
      service.execute(this::buildCustomRuntimeImage);
      service.shutdown();
      try {
        service.awaitTermination(1, TimeUnit.DAYS);
      } catch (InterruptedException e) {
        Thread.interrupted();
        // return;
      }
    }
  }

  public void buildMainModules() {
    var units = main().units();
    bach.logbook().log(Level.DEBUG, "Build of %d main module(s) started", units.size());
    bach.executeCall(computeJavacForMainSources());
    Paths.createDirectories(base().modules(""));
    units.toUnits().map(this::computeJarForMainModule).forEach(bach::executeCall);
  }

  public void buildApiDocumentation() {
    bach.executeCall(computeJavadocForMainSources());
    bach.executeCall(computeJarForApiDocumentation());
  }

  public void buildCustomRuntimeImage() {
    var modulePaths = modulePaths(base().modules(""), base().libraries());
    var autos = Modules.findAutomaticModules(modulePaths);
    if (autos.size() > 0) {
      bach.logbook().log(Level.WARNING, "Automatic module(s) detected: %s", autos);
      return;
    }
    Paths.deleteDirectories(base().workspace("image"));
    var jlink = computeJLinkForCustomRuntimeImage();
    bach.executeCall(jlink);
  }

  public Javac computeJavacForMainSources() {
    var release = main().release().feature();
    var modulePath = modulePath(base().libraries());
    return Call.javac()
        .withModule(main().units().units().keySet())
        .with("--module-version", project().version())
        .with(moduleSourcePaths(main().units(), false), Javac::withModuleSourcePath)
        .with(modulePath, Javac::withModulePath)
        .withEncoding("UTF-8")
        .with("-parameters")
        .withRecommendedWarnings()
        .with("-Werror")
        .with("--release", release)
        .with("-d", base().classes("", release));
  }

  public Jar computeJarForMainModule(SourceUnit unit) {
    var module = unit.name();
    var archive = base().modules("").resolve(module + '@' + project().version() + ".jar");
    var mainClass = unit.descriptor().mainClass();
    var release = main().release().feature();
    return Call.jar()
        .with("--create")
        .withArchiveFile(archive)
        .with(mainClass.isPresent(), "--main-class", mainClass.orElse("?"))
        .with("-C", base().classes("", release, module), ".")
        .with(unit.sources().directories(), (jar, src) -> jar.with("-C", src.path(), "."));
  }

  public Javadoc computeJavadocForMainSources() {
    var release = main().release().feature();
    var modulePath = modulePath(base().libraries());
    return Call.javadoc()
        .withModule(main().units().units().keySet())
        .with(moduleSourcePaths(main().units(), false), Javadoc::withModuleSourcePath)
        .with(modulePath, Javadoc::withModulePath)
        .with("-d", base().documentation("api"))
        .withEncoding("UTF-8")
        .with("-locale", "en")
        .with("-quiet")
        .with("-Xdoclint")
        .with("-Xwerror") // https://bugs.openjdk.java.net/browse/JDK-8237391
        .with("--show-module-contents", "all")
        .with("-link", "https://docs.oracle.com/en/java/javase/" + release + "/docs/api");
  }

  public Jar computeJarForApiDocumentation() {
    var file = project().toNameAndVersion() + "-api.jar";
    return Call.jar()
        .with("--create")
        .withArchiveFile(base().documentation(file))
        .with("--no-manifest")
        .with("-C", base().documentation("api"), ".");
  }

  public Call<?> computeJLinkForCustomRuntimeImage() {
    var mainModule = Modules.findMainModule(main().units().toUnits().map(SourceUnit::descriptor));
    return Call.tool("jlink")
        .with("--add-modules", main().units().toNames(","))
        .with("--module-path", modulePath(base().modules(""), base().libraries()).get(0))
        .with(mainModule.isPresent(), "--launcher", project().name() + '=' + mainModule.orElse("?"))
        .with("--compress", "2")
        .with("--no-header-files")
        .with("--no-man-pages")
        .with("--output", base().workspace("image"));
  }

  public static List<String> modulePath(Path... elements) {
    var paths = modulePaths(elements);
    return paths.isEmpty() ? List.of() : List.of(Paths.join(paths));
  }

  public static List<Path> modulePaths(Path... elements) {
    var paths = new ArrayList<Path>();
    for (var element : elements) if (Files.exists(element)) paths.add(element);
    return List.copyOf(paths);
  }

  public List<String> moduleSourcePaths(SourceUnits units, boolean forceModuleSpecificForm) {
    var paths = new ArrayList<String>();
    var patterns = new TreeSet<String>(); // "src:etc/*/java"
    var specific = new TreeMap<String, List<Path>>(); // "foo=java:java-9"
    for (var unit : units.units().values()) {
      var sourcePaths = moduleSpecificSourcePaths(unit.sources().directories());
      if (forceModuleSpecificForm) {
        specific.put(unit.name(), sourcePaths);
        continue;
      }
      try {
        for (var path : sourcePaths) patterns.add(moduleSourcePathPatternForm(path, unit.name()));
      } catch (FindException e) {
        specific.put(unit.name(), sourcePaths);
      }
    }
    if (patterns.isEmpty() && specific.isEmpty()) throw new IllegalStateException("No forms?!");
    if (!patterns.isEmpty()) paths.add(String.join(File.pathSeparator, patterns));
    var entries = specific.entrySet();
    for (var entry : entries) paths.add(entry.getKey() + "=" + Paths.join(entry.getValue()));
    return List.copyOf(paths);
  }

  public String moduleSourcePathPatternForm(Path info, String module) {
    var deque = new ArrayDeque<String>();
    for (var element : info.normalize()) {
      var name = element.toString();
      if (name.equals("module-info.java")) continue;
      deque.addLast(name.equals(module) ? "*" : name);
    }
    var pattern = String.join(File.separator, deque);
    if (!pattern.contains("*")) throw new FindException("Name '" + module + "' not found: " + info);
    if (pattern.equals("*")) return ".";
    if (pattern.endsWith("*")) return pattern.substring(0, pattern.length() - 2);
    if (pattern.startsWith("*")) return "." + File.separator + pattern;
    return pattern;
  }

  public List<Path> moduleSpecificSourcePaths(Set<SourceDirectory> sources) {
    var s0 = sources.iterator().next();
    if (s0.isModuleInfoJavaPresent()) return List.of(s0.path());
    for (var source : sources)
      if (source.isModuleInfoJavaPresent()) return List.of(s0.path(), source.path());
    throw new IllegalStateException("No module-info.java found in: " + sources);
  }
}
