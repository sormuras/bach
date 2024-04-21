/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import run.bach.ToolCall;
import run.bach.workflow.Structure.DeclaredModule;
import run.bach.workflow.Structure.Space;

/** Package compiled classes and resources into modular Java archive files. */
public interface ModulesCompiler extends Action {
  default void compileModules(Space space) {
    var compilerCalls = new ArrayList<ToolCall>(); // javac --release N ...
    var archiverCalls = new ArrayList<ToolCall>(); // jar --create --file ...
    var modules = workflow().folders().out(space.name(), "modules");
    for (var module : space.modules()) {
      var jar = modulesCompilerNewJarToolCall();
      jar = jar.add("--create");
      jar = modulesCompilerWithFile(jar, modules, module);
      jar = modulesCompilerWithModuleVersion(jar);
      jar = modulesCompilerWithDate(jar);
      jar = modulesCompilerWithLauncher(jar, space, module);
      jar = modulesCompilerWithBaseClassesAndResources(jar, space, module);
      jar = modulesCompilerWithClassesOfPatchedModule(jar, space, module);
      jar = modulesCompilerWithTargetedClassesAndResources(jar, space, module, compilerCalls);
      archiverCalls.add(jar);
    }

    compilerCalls.stream().parallel().forEach(this::modulesCompilerRunJavacToolCall);
    archiverCalls.stream().parallel().forEach(this::modulesCompilerRunJarToolCall);
  }

  default ToolCall modulesCompilerNewJarToolCall() {
    return ToolCall.of("jar");
  }

  default void modulesCompilerRunJarToolCall(ToolCall call) {
    run(call);
  }

  default void modulesCompilerRunJavacToolCall(ToolCall call) {
    run(call);
  }

  default ToolCall modulesCompilerWithFile(ToolCall jar, Path modules, DeclaredModule module) {
    var file = modules.resolve(module.name() + ".jar");
    return jar.add("--file", file);
  }

  default ToolCall modulesCompilerWithModuleVersion(ToolCall jar) {
    return jar.add("--module-version", workflow().structure().basics().version());
  }

  default ToolCall modulesCompilerWithDate(ToolCall jar) {
    if (Runtime.version().feature() < 19) return jar;
    var basics = workflow().structure().basics();
    var timestamp = basics.timestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    return jar.add("--date", timestamp);
  }

  default ToolCall modulesCompilerWithLauncher(ToolCall jar, Space space, DeclaredModule module) {
    if (space.launchers().isEmpty()) return jar;
    var launcher = space.launchers().getFirst();
    var name = module.name();
    if (!launcher.startsWith(name + '/')) return jar;
    var className = launcher.substring(name.length() + 1);
    return jar.add("--main-class", className);
  }

  default ToolCall modulesCompilerWithBaseClassesAndResources(
      ToolCall jar, Space space, DeclaredModule module) {
    var folders = workflow().folders();
    var classes = folders.out(space.name(), "classes");
    var release0 = space.targets();
    var feature0 = Runtime.version().feature();
    var classes0 = classes.resolve("java-" + release0.orElse(feature0));
    var name = module.name();
    if (Files.isDirectory(classes0.resolve(name))) {
      jar = jar.add("-C", classes0.resolve(name), ".");
    }
    for (var resources : module.base().resources()) {
      jar = jar.add("-C", resources, ".");
    }
    return jar;
  }

  default ToolCall modulesCompilerWithClassesOfPatchedModule(
      ToolCall jar, Space space, DeclaredModule module) {
    var folders = workflow().folders();
    var spaces = workflow().structure().spaces();
    var name = module.name();
    for (var requires : space.requires()) {
      var required = spaces.space(requires);
      if (required.modules().find(name).isPresent()) {
        var javaR = "java-" + required.targets().orElse(Runtime.version().feature());
        jar = jar.add("-C", folders.out(requires, "classes", javaR, name), ".");
      }
    }
    return jar;
  }

  default ToolCall modulesCompilerWithTargetedClassesAndResources(
      ToolCall jar, Space space, DeclaredModule module, ArrayList<ToolCall> releases) {
    var name = module.name();
    var classes = workflow().folders().out(space.name(), "classes");
    var release0 = space.targets();
    var feature0 = Runtime.version().feature();
    var classes0 = classes.resolve("java-" + release0.orElse(feature0));
    for (var release : module.targeted().keySet().stream().sorted().toList()) {
      var folders = module.targeted().get(release);
      for (var sources : folders.sources()) {
        var classesR = classes.resolve("java-" + release).resolve(name);
        var javac = ToolCall.of("javac").add("--release", release);
        var modulePath = space.toModulePath(workflow().folders());
        if (modulePath.isPresent()) {
          javac = javac.add("--module-path", modulePath.get());
          javac = javac.add("--processor-module-path", modulePath.get());
        }
        javac =
            javac
                .add("--class-path", classes0.resolve(name))
                .add("-implicit:none")
                .add("-d", classesR)
                .addFiles(sources, "**.java");
        releases.add(javac);
        jar = jar.add("--release", release).add("-C", classesR, ".");
      }
      var needsReleaseArgument = folders.sources().isEmpty() && !folders.resources().isEmpty();
      if (needsReleaseArgument) jar = jar.add("--release", release);
      for (var resources : folders.resources()) {
        jar = jar.add("-C", resources, ".");
      }
    }
    return jar;
  }
}
