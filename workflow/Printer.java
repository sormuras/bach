/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.workflow;

import java.io.PrintWriter;
import java.util.List;

public interface Printer extends Action {
  enum PrinterTopic {
    HELP,
    STATUS,
    VERSIONS
  }

  default void print(PrinterTopic... topics) {
    for (var topic : topics.length >= 1 ? List.of(topics) : printerUsesDefaultTopics()) {
      switch (topic) {
        case HELP -> printHelp();
        case STATUS -> printStatus();
        case VERSIONS -> printVersions();
      }
    }
  }

  default void printHelp() {}

  default void printStatus() {
    @SuppressWarnings("resource")
    var printer = printerUsesWriterForOutput();
    printer.println(workflow().folders());
    printer.println(workflow().structure());
    printer.println(workflow().runner());
  }

  default void printVersions() {}

  default List<PrinterTopic> printerUsesDefaultTopics() {
    return List.of(PrinterTopic.STATUS);
  }

  default PrintWriter printerUsesWriterForOutput() {
    return new PrintWriter(System.out, true);
  }
}
