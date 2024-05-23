/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.nio.file.Path;
import java.util.Set;
import run.bach.ModuleResolver;
import run.bach.internal.ModulesSupport;
import run.bach.workflow.Structure.DeclaredModules;
import run.bach.workflow.Structure.Space;

public interface Restorer extends Action {
  default void restore() {
    say("Restoring required modules ...");
    var lib = restorerUsesLibraryDirectory();
    var finder = workflow().structure().libraries();
    var resolver = ModuleResolver.ofSingleDirectory(lib, finder);
    restorerUsesModuleNames().forEach(resolver::resolveModule);
    say("Restoring missing modules recursively ...");
    resolver.resolveMissingModules();
  }

  default Path restorerUsesLibraryDirectory() {
    return workflow().folders().root("lib");
  }

  default Set<String> restorerUsesModuleNames() {
    return restorerFindMissingModuleNames();
  }

  default Set<String> restorerFindMissingModuleNames() {
    var spaces = workflow().structure().spaces();
    var finders =
        spaces.list().stream().map(Space::modules).map(DeclaredModules::toModuleFinder).toList();
    var missing = ModulesSupport.listMissingNames(finders, Set.of());
    if (missing.isEmpty()) return Set.of();
    return Set.copyOf(missing);
  }
}
