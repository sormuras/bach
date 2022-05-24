package com.github.sormuras.bach;

import java.util.function.UnaryOperator;

public interface ToolCallTweak extends UnaryOperator<ToolCall> {

  String WORKFLOW_COMPILE_CLASSES_JAVAC = "com.github.sormuras.bach/compile-classes::javac";
  String WORKFLOW_TEST_JUNIT = "com.github.sormuras.bach/test::junit";

  static ToolCallTweak identity() {
    return call -> call;
  }

  default ToolCallTweak merged(ToolCallTweak after) {
    return tweak -> after.apply(apply(tweak));
  }
}
