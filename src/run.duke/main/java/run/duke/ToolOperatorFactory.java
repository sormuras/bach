package run.duke;

@FunctionalInterface
public interface ToolOperatorFactory {
  ToolOperator createToolOperator(ToolRunner runner);
}
