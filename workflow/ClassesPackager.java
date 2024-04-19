/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.workflow.Structure.DeclaredModule;
import run.bach.workflow.Structure.Space;

/** Package compiled classes and resources into modular Java archive files. */
public interface ClassesPackager extends Action {
  default void packageClasses(Space space) {
    var calls = new TreeMap<String, List<ToolCall>>();
    for (var module : space.modules()) {
      var context = new Context(workflow().folders(), space, module, calls);
      var jar = packageClassesCreateJarCall();
      jar = jar.add("--create");
      jar = packageClassesWithFile(jar, context);
      jar = packageClassesWithModuleVersion(jar);
      jar = packageClassesWithDate(jar);
      jar = packageClassesWithLauncher(jar, context);
      jar = packageClassesWithBaseClassesAndResources(jar, context);
      jar =
          packageClassesWithClassesOfPatchedModule(
              jar, context); // instead of "--patch-module" at runtime
      jar = packageClassesWithTargetedClassesAndResources(jar, context);
      context.withJarCall(jar);
    }
    for (var list : calls.values()) {
      list.stream().parallel().forEach(workflow().runner()::run);
    }
  }

  default ToolCall packageClassesCreateJarCall() {
    return ToolCall.of("jar");
  }

  default ToolCall packageClassesWithFile(ToolCall jar, Context context) {
    var file = context.modules().resolve(context.module().name() + ".jar");
    return jar.add("--file", file);
  }

  default ToolCall packageClassesWithModuleVersion(ToolCall jar) {
    return jar.add("--module-version", workflow().structure().basics().version());
  }

  default ToolCall packageClassesWithDate(ToolCall jar) {
    if (Runtime.version().feature() < 19) return jar;
    var basics = workflow().structure().basics();
    var timestamp = basics.timestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    return jar.add("--date", timestamp);
  }

  default ToolCall packageClassesWithLauncher(ToolCall jar, Context context) {
    var space = context.space();
    if (space.launchers().isEmpty()) return jar;
    var launcher = space.launchers().getFirst();
    var name = context.module().name();
    if (!launcher.startsWith(name + '/')) return jar;
    var className = launcher.substring(name.length() + 1);
    return jar.add("--main-class", className);
  }

  default ToolCall packageClassesWithBaseClassesAndResources(ToolCall jar, Context context) {
    var release0 = context.space().targets();
    var feature0 = Runtime.version().feature();
    var classes0 = context.classes().resolve("java-" + release0.orElse(feature0));
    var name = context.module().name();
    if (Files.isDirectory(classes0.resolve(name))) {
      jar = jar.add("-C", classes0.resolve(name), ".");
    }
    for (var resources : context.module().base().resources()) {
      jar = jar.add("-C", resources, ".");
    }
    return jar;
  }

  default ToolCall packageClassesWithClassesOfPatchedModule(ToolCall jar, Context context) {
    var folders = workflow().folders();
    var spaces = workflow().structure().spaces();
    var name = context.module().name();
    for (var requires : context.space().requires()) {
      var required = spaces.space(requires);
      if (required.modules().find(name).isPresent()) {
        var javaR = "java-" + required.targets().orElse(Runtime.version().feature());
        jar = jar.add("-C", folders.out(requires, "classes", javaR, name), ".");
      }
    }
    return jar;
  }

  default ToolCall packageClassesWithTargetedClassesAndResources(ToolCall jar, Context context) {
    var module = context.module();
    var name = context.module().name();
    var release0 = context.space().targets();
    var feature0 = Runtime.version().feature();
    var classes0 = context.classes().resolve("java-" + release0.orElse(feature0));
    for (var release : module.targeted().keySet().stream().sorted().toList()) {
      var folders = module.targeted().get(release);
      for (var sources : folders.sources()) {
        var classesR = context.classes().resolve("java-" + release).resolve(name);
        var javac = ToolCall.of("javac").add("--release", release);
        var modulePath = context.space().toModulePath(workflow().folders());
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
        context.withJavacCall(javac);
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

  record Context(
      Bach.Folders folders, Space space, DeclaredModule module, Map<String, List<ToolCall>> calls) {
    public Path classes() {
      return folders.out(space().name(), "classes");
    }

    public Path modules() {
      return folders.out(space().name(), "modules");
    }

    public void withJavacCall(ToolCall javac) {
      calls.computeIfAbsent("10 javac calls", _ -> new ArrayList<>()).add(javac);
    }

    public void withJarCall(ToolCall jar) {
      calls.computeIfAbsent("20 jar calls", _ -> new ArrayList<>()).add(jar);
    }
  }
}
