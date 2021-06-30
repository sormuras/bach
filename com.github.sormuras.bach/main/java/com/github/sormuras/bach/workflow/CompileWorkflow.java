package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Workflow;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public abstract class CompileWorkflow extends Workflow {

  protected final Space space;

  public CompileWorkflow(Bach bach, Space space) {
    super(bach);
    this.space = space;
  }

  @Override
  public void execute() {
    var name = space.name();
    if (space.modules().isEmpty()) {
      bach.log("No %s module present, nothing to compile.", name);
      return;
    }
    var size = space.modules().size();
    bach.log(Level.INFO, "Compile %d %s module%s", size, name, size == 1 ? "" : "s");

    var calls = generateCalls(DeclaredModules.of(space.modules()));
    calls.forEach(call -> bach.log(Level.INFO, call));
  }

  public List<String> generateCalls(DeclaredModules modules) {
    var calls = new ArrayList<String>();
    calls.add(generateJavacCall(modules.names().toList()));
    modules.descriptors().map(this::generateJarCall).forEach(calls::add);
    return calls;
  }

  public String generateJavacCall(List<String> modules) {
    var joiner = new StringJoiner(" ");
    joiner.add("javac");
    joiner.add("--module").add(String.join(",", modules));
    project
        .mainModules()
        .release()
        .ifPresent(release -> joiner.add("--release").add("" + release.feature()));
    return joiner.toString();
  }

  public String generateJarCall(ModuleDescriptor module) {
    var version = module.version().orElse(project.version().value());
    var file = module.name() + "@" + version + ".jar";
    var joiner = new StringJoiner(" ");
    joiner.add("jar");
    joiner.add("--create");
    joiner.add("--file").add(file);
    joiner.add("--module-version").add(version.toString());
    module.mainClass().ifPresent(name -> joiner.add("--main-class").add(name));
    return joiner.toString();
  }
}
