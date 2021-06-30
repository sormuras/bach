package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.stream.Stream;

public record Printer(Bach bach) {
  public void printAllModules() {
    printDeclaredModules();
    printExternalModules();
    printSystemModules();
  }

  public void printDeclaredModules() {
    var out = bach.settings().logbook().out();
    out.println("Declared Modules");
    var main = DeclaredModules.of(bach.project().mainModules().set());
    var test = DeclaredModules.of(bach.project().testModules().set());
    if (main.isEmpty() && test.isEmpty()) {
      out.println("  -");
      return;
    }
    if (!main.isEmpty()) {
      out.println("  Main Code Space");
      streamModuleSummaryLines(main).forEach(line -> out.printf("    %s%n", line));
    }
    if (!test.isEmpty()) {
      out.println("  Test Code Space");
      streamModuleSummaryLines(test).forEach(line -> out.printf("    %s%n", line));
    }
  }

  public void printExternalModules() {
    var finder = ModuleFinder.of(bach.settings().folders().externalModules());
    var out = bach.settings().logbook().out();
    out.println("External Modules");
    if (finder.findAll().isEmpty()) out.println("  -");
    else streamModuleSummaryLines(finder).forEach(line -> out.printf("  %s%n", line));
  }

  public void printSystemModules() {
    var finder = ModuleFinder.ofSystem();
    var out = bach.settings().logbook().out();
    out.println("System Modules");
    streamModuleSummaryLines(finder).forEach(line -> out.printf("  %s%n", line));
  }

  Stream<String> streamModuleSummaryLines(ModuleFinder finder) {
    return finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .sorted()
        .map(Printer::toModuleSummaryLine);
  }

  static String toModuleSummaryLine(ModuleDescriptor descriptor) {
    return descriptor.toNameAndVersion();
  }
}
