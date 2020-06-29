package de.sormuras.bach;

public class Workflow {

  public static Workflow ofSystem() {
    return new Workflow();
  }

  public void build(Bach bach) throws Exception {
    bach.logbook().log(System.Logger.Level.WARNING, "Default build workflow not implemented, yet.");
  }
}
