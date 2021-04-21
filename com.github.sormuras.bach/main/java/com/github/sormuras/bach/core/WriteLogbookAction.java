package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.Action;

public class WriteLogbookAction extends BachAction {

  public WriteLogbookAction(Bach bach) {
    super(Action.WRITE_LOGBOOK, bach);
  }

  public void write() {
    bach().say("Logbook written to ... TODO");
  }
}