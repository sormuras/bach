package test.base.magnificat.core;

import test.base.magnificat.Bach;
import test.base.magnificat.api.Action;

public class WriteLogbookAction extends BachAction {

  public WriteLogbookAction(Bach bach) {
    super(Action.WRITE_LOGBOOK, bach);
  }

  public void write() {}
}
