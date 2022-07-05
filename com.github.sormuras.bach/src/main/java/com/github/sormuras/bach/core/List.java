package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Tool;
import com.github.sormuras.bach.ToolFinder;
import com.github.sormuras.bach.ToolOperator;
import com.github.sormuras.bach.internal.ModulesSupport;
import com.github.sormuras.bach.project.DeclaredModules;
import com.github.sormuras.bach.project.ProjectSpace;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

public class List implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    var set = Set.of(args);
    var all = set.isEmpty();
    if (all || set.contains("system-modules")) {
      if (set.size() != 1) out.println("\n## bach list system-modules");
      var names = names(ModuleFinder.ofSystem());
      names.forEach(out::println);
      out.printf("  %d system module%s%n", names.size(), names.size() == 1 ? "" : "s");
    }
    if (all || set.contains("external-modules")) {
      if (set.size() != 1) out.println("\n## bach list external-modules");
      var path = bach.configuration().paths().externalModules();
      var names = names(ModuleFinder.of(path));
      names.forEach(out::println);
      var size = names.size();
      out.printf("  %d external module%s in %s%n", size, size == 1 ? "" : "s", path);
    }
    var project = bach.project();
    if (all || set.contains("project-modules")) {
      if (set.size() != 1) out.println("\n## bach list project-modules");
      var total = project.modules().size();
      out.printf("project declares %d module%s%n", total, total == 1 ? "" : "s");
      for (var space : project.spaces().list()) {
        var names = space.modules().names();
        if (names.isEmpty()) continue;
        names.forEach(out::println);
        var size = names.size();
        out.printf("  %d module%s in %s space%n", size, size == 1 ? "" : "s", space.name());
      }
    }
    if (all || set.contains("missing-modules")) {
      var finders = new ArrayList<ModuleFinder>();
      finders.add(ModuleFinder.of(bach.configuration().paths().externalModules()));
      finders.addAll(
          project.spaces().list().stream()
              .map(ProjectSpace::modules)
              .map(DeclaredModules::toModuleFinder)
              .toList());
      var modules = ModulesSupport.listMissingModules(finders, project.externals().requires());
      if (!modules.isEmpty()) {
        if (set.size() != 1) out.println("\n## bach list missing-modules");
        modules.forEach(out::println);
        var size = modules.size();
        out.printf("  %d missing module%s%n", size, size == 1 ? "" : "s");
      }
    }
    if (all || set.contains("tools")) {
      if (set.size() != 1) out.println("\n## bach list tools");
      out.println(formatTools(bach.configuration().finder()));
    }
    return 0;
  }

  private static java.util.List<String> names(ModuleFinder finder) {
    return finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::toNameAndVersion)
        .sorted()
        .toList();
  }

  private static String formatTools(ToolFinder finder) {
    var names = new TreeMap<String, java.util.List<Tool>>();
    for (var tool : finder.findAll()) {
      var name = tool.name().substring(tool.name().lastIndexOf('/') + 1);
      names.computeIfAbsent(name, key -> new ArrayList<>()).add(tool);
    }
    var lines = new ArrayList<String>();
    for (var entry : names.entrySet()) {
      var name = entry.getKey();
      var tools = entry.getValue();
      var first = tools.get(0);
      lines.add(formatLine("%20s -> %s [%s]", name, first));
      tools.stream().skip(1).forEach(tool -> lines.add(formatLine("%20s    %s [%s]", "", tool)));
    }
    return String.join("\n", lines);
  }

  private static String formatLine(String format, String name, Tool tool) {
    return format.formatted(name, tool.name(), tool.provider().getClass().getSimpleName());
  }
}
