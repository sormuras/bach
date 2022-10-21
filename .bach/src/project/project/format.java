package project;

import run.bach.ToolOperator;

public final class format implements ToolOperator {
  @Override
  public void run(Operation operation) {
    var name = "google-java-format@1.15.0";
    operation.run("install", name);
    operation.run(name, format -> format.with("--replace").withFindFiles("**.java"));
  }
}
