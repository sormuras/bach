package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;

public class WriteLogbookAction extends BachAction {

  public WriteLogbookAction(Bach bach) {
    super(bach);
  }

  public void write() {
    bach().say("Logbook written to ... TODO");
  }
}