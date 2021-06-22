package com.github.sormuras.bach;

public class Builder {

  @FunctionalInterface
  public interface Factory {
    Builder newBuilder(Bach bach, Project project);
  }

  protected final Bach bach;
  protected final Project project;

  public Builder(Bach bach, Project project) {
    this.bach = bach;
    this.project = project;
  }

  public void build() {
    compileMainModules();
    compileTestModules();
  }

  public void compileMainModules() {
    newCompileMainModulesWorkflow().execute();
  }

  public void compileTestModules() {
    newCompileTestModulesWorkflow().execute();
  }

  protected CompileMainModulesWorkflow newCompileMainModulesWorkflow() {
    return new CompileMainModulesWorkflow(bach, project);
  }

  protected CompileTestModulesWorkflow newCompileTestModulesWorkflow() {
    return new CompileTestModulesWorkflow(bach, project);
  }
}
