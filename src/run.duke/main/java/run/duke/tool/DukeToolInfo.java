package run.duke.tool;

import run.duke.Tool;
import run.duke.ToolInfo;
import run.duke.ToolOperatorFactory;
import run.duke.ToolRunner;

enum DukeToolInfo implements ToolInfo {
  LIST(ListTool.NAME, ListTool::new);

  final String namespace;
  final String nickname;
  final String identifier;
  final ToolOperatorFactory factory;

  DukeToolInfo(String nickname, ToolOperatorFactory factory) {
    this.namespace = Tool.namespace(getClass());
    this.nickname = nickname;
    this.identifier = Tool.identifier(namespace, nickname);
    this.factory = factory;
  }

  @Override
  public String identifier() {
    return identifier;
  }

  @Override
  public String namespace() {
    return namespace;
  }

  @Override
  public String nickname() {
    return nickname;
  }

  @Override
  public Tool tool(ToolRunner runner) {
    return new Tool(factory.createToolOperator(runner));
  }
}
