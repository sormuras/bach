package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public record Printer(Bach bach) {
  public void printAllModules() {
    printDeclaredModules();
    printExternalModules();
    printSystemModules();
  }

  public void printDeclaredModules() {
    for (var space : bach.project().spaces().stream().toList()) {
      var finder = DeclaredModuleFinder.of(space.modules());
      printModules("Declared Modules in Project Space: " + space.name(), finder);
    }
  }

  public void printExternalModules() {
    printModules("External Modules", ModuleFinder.of(bach.folders().externalModules()));
  }

  public void printSystemModules() {
    printModules("System Modules", ModuleFinder.ofSystem());
  }

  public void printModules(String caption, ModuleFinder finder) {
    var out = bach.logbook().out();
    out.println(caption);
    if (finder.findAll().isEmpty()) out.println("  -");
    else streamModuleSummaryLines(finder).forEach(line -> out.printf("  %s%n", line));
  }

  public void printTools() {
    var out = bach.logbook().out();
    bach.runner().streamToolProviders().map(ToolProvider::name).sorted().forEach(out::println);
  }

  static Stream<String> streamModuleSummaryLines(ModuleFinder finder) {
    return finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .sorted()
        .map(Printer::toModuleSummaryLine);
  }

  static String toModuleSummaryLine(ModuleDescriptor descriptor) {
    return descriptor.toNameAndVersion();
  }
}
