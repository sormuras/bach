/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import run.bach.ToolCall;
import run.bach.workflow.Structure.DeclaredModule;
import run.bach.workflow.Structure.Space;

/** Package compiled classes and resources into modular Java archive files. */
public interface ModulesCompiler extends Action {
  // TODO Replace with java.lang.ScopedValue of https://openjdk.org/jeps/464
  InheritableThreadLocal<Space> SPACE = new InheritableThreadLocal<>();
  InheritableThreadLocal<DeclaredModule> MODULE = new InheritableThreadLocal<>();

  static Space space() {
    return Optional.ofNullable(SPACE.get()).orElseThrow(IllegalStateException::new);
  }

  static DeclaredModule module() {
    return Optional.ofNullable(MODULE.get()).orElseThrow(IllegalStateException::new);
  }

  default void compileModules(Space space) {
    if (SPACE.get() != null) throw new IllegalStateException();
    if (MODULE.get() != null) throw new IllegalStateException();
    try {
      SPACE.set(space);
      var compilerCalls = new ArrayList<ToolCall>(); // javac --release N ...
      var archiverCalls = new ArrayList<ToolCall>(); // jar --create --file ...
      for (var module : space.modules()) {
        MODULE.set(module);
        var jar = modulesCompilerUsesJarToolCall();
        jar = modulesCompilerWithCreateMode(jar);
        jar = modulesCompilerWithFile(jar);
        jar = modulesCompilerWithModuleVersion(jar);
        jar = modulesCompilerWithDate(jar);
        jar = modulesCompilerWithLauncher(jar);
        jar = modulesCompilerWithBaseClassesAndResources(jar);
        jar = modulesCompilerWithClassesOfPatchedModule(jar);
        jar = modulesCompilerWithTargetedClassesAndResources(jar, compilerCalls::add);
        archiverCalls.add(jar);
      }
      compilerCalls.stream().parallel().forEach(this::modulesCompilerRunJavacToolCall);
      archiverCalls.stream().parallel().forEach(this::modulesCompilerRunJarToolCall);
    } finally {
      SPACE.remove();
      MODULE.remove();
    }
  }

  default ToolCall modulesCompilerUsesJarToolCall() {
    return ToolCall.of("jar");
  }

  default ToolCall modulesCompilerUsesJavacToolCall() {
    return ToolCall.of("javac");
  }

  default ToolCall modulesCompilerWithCreateMode(ToolCall jar) {
    return jar.add("--create");
  }

  default ToolCall modulesCompilerWithFile(ToolCall jar) {
    var modules = workflow().folders().out(space().name(), "modules");
    var file = modules.resolve(module().name() + ".jar");
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

  default ToolCall modulesCompilerWithLauncher(ToolCall jar) {
    var space = space();
    if (space.launchers().isEmpty()) return jar;
    var launcher = space.launchers().getFirst(); // <name> + '=' + <module>[/<main-class>]
    if (!launcher.module().equals(module().name())) return jar;
    return launcher.mainClass().map(main -> jar.add("--main-class", main)).orElse(jar);
  }

  default ToolCall modulesCompilerWithBaseClassesAndResources(ToolCall jar) {
    var space = space();
    var module = module();
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

  default ToolCall modulesCompilerWithClassesOfPatchedModule(ToolCall jar) {
    var folders = workflow().folders();
    var spaces = workflow().structure().spaces();
    var name = module().name();
    for (var requires : space().requires()) {
      var required = spaces.space(requires);
      if (required.modules().find(name).isPresent()) {
        var javaR = "java-" + required.targets().orElse(Runtime.version().feature());
        jar = jar.add("-C", folders.out(requires, "classes", javaR, name), ".");
      }
    }
    return jar;
  }

  default ToolCall modulesCompilerWithTargetedClassesAndResources(
      ToolCall jar, Consumer<ToolCall> consumer) {
    var space = space();
    var module = module();
    var name = module.name();
    var classes = workflow().folders().out(space.name(), "classes");
    var release0 = space.targets();
    var feature0 = Runtime.version().feature();
    var classes0 = classes.resolve("java-" + release0.orElse(feature0));
    for (var release : module.targeted().keySet().stream().sorted().toList()) {
      var folders = module.targeted().get(release);
      for (var sources : folders.sources()) {
        var classesR = classes.resolve("java-" + release).resolve(name);
        var javac = modulesCompilerUsesJavacToolCall();
        javac = javac.add("--release", release);
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
        consumer.accept(javac);
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

  default void modulesCompilerRunJarToolCall(ToolCall jar) {
    run(jar);
  }

  default void modulesCompilerRunJavacToolCall(ToolCall javac) {
    run(javac);
  }
}
