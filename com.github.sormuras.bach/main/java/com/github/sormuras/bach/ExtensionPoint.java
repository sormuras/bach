package com.github.sormuras.bach;

import java.util.ServiceLoader;

public interface ExtensionPoint {

  interface BeginOfWorkflowExecution extends ExtensionPoint {

    record Event(Bach bach) {}

    void onBeginOfWorkflowExecution(Event event);

    static void fire(Bach bach) {
      var event = new Event(bach);
      ServiceLoader.load(bach.core().layer(), BeginOfWorkflowExecution.class)
          .forEach(service -> service.onBeginOfWorkflowExecution(event));
    }
  }

  interface EndOfWorkflowExecution extends ExtensionPoint {

    record Event(Bach bach) {}

    void onEndOfWorkflowExecution(Event context);

    static void fire(Bach bach) {
      var context = new Event(bach);
      ServiceLoader.load(bach.core().layer(), EndOfWorkflowExecution.class)
          .forEach(service -> service.onEndOfWorkflowExecution(context));
    }
  }
}
