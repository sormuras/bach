/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.util.List;
import run.bach.workflow.Structure.Space;

public interface Tester extends Action, JavaTester, JUnitTester, ToolTester {
  default void test() {
    var spaces = testerUsesSpacesForTesting();
    if (spaces.isEmpty()) {
      say("No module space selected for testing; nothing to test.");
      return;
    }
    var names = spaces.stream().map(Space::name).toList();
    var size = names.size();
    say("Testing %d module space%s %s ...".formatted(size, size == 1 ? "" : "s", names));
    for (var space : spaces) test(space);
  }

  default List<Space> testerUsesSpacesForTesting() {
    return workflow().structure().spaces().list().stream()
        .filter(space -> space.name().equals("test"))
        .toList();
  }

  default void test(Space space) {
    testViaJava(space); // java --module $MODULE
    testViaTool(space); // ToolProvider::run $MODULE/test.*
    testViaJUnit(space); // junit --select-module $MODULE
  }
}
