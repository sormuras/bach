package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.DeclaredModules;

import java.lang.module.ModuleDescriptor;
import java.util.StringJoiner;

public class CompileTestModulesWorkflow extends Workflow {

  public CompileTestModulesWorkflow(Bach bach, Project project) {
    super(bach, project);
  }

  @Override
  public void execute() {
    var modules = DeclaredModules.of(project.testModules().set());
    System.out.println("javac --module " + String.join(",", modules.names().toList()));
    modules.descriptors().map(this::jar).forEach(System.out::println);
  }

  public String jar(ModuleDescriptor module) {
    var version = module.version().orElse(project.version().value());
    var file = module.name() + "@" + version + "-test.jar";
    var joiner = new StringJoiner(" ");
    joiner.add("jar");
    joiner.add("--create");
    joiner.add("--file").add(file);
    joiner.add("--module-version").add(version.toString());
    module.mainClass().ifPresent(it -> joiner.add("--main-class").add(it));
    return joiner.toString();
  }
}
