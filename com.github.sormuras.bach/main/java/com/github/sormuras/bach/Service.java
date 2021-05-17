package com.github.sormuras.bach;

import java.util.ServiceLoader;

public interface Service {

  interface BeginOfWorkflowExecution extends Service {

    record Event(Bach bach) {}

    void onBeginOfWorkflowExecution(Event event);

    static void fire(Bach bach) {
      var event = new Event(bach);
      ServiceLoader.load(bach.configuration().layer(), BeginOfWorkflowExecution.class)
          .forEach(service -> service.onBeginOfWorkflowExecution(event));
    }
  }

  interface EndOfWorkflowExecution extends Service {

    record Event(Bach bach) {}

    void onEndOfWorkflowExecution(Event context);

    static void fire(Bach bach) {
      var context = new Event(bach);
      ServiceLoader.load(bach.configuration().layer(), EndOfWorkflowExecution.class)
          .forEach(service -> service.onEndOfWorkflowExecution(context));
    }
  }
}
