package run.duke;

@FunctionalInterface
public interface WorkpieceFactory {
  <R extends Record> R createWorkpiece(Workbench workbench);
}
