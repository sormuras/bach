package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Workflow;
import com.github.sormuras.bach.call.JarCall;
import com.github.sormuras.bach.call.JavacCall;
import java.lang.module.ModuleDescriptor;
import java.util.List;

public abstract class CompileWorkflow extends Workflow {

  protected final Space space;

  public CompileWorkflow(Bach bach, Space space) {
    super(bach);
    this.space = space;
  }

  @Override
  public void execute() {
    bach.execute(generateCallTree());
  }

  @Override
  public Call.Tree generateCallTree() {
    var name = space.name();
    if (space.modules().isEmpty()) return Call.tree("No %s module present".formatted(name));
    return generateCallTree(DeclaredModules.of(space.modules()));
  }

  public Call.Tree generateCallTree(DeclaredModules modules) {
    var size = space.modules().size();
    var suffix = "%d %s module%s".formatted(size, space.name(), size == 1 ? "" : "s");
    var javacCall = generateJavacCall(modules.names().toList());
    var jarCalls = modules.descriptors().parallel().map(this::generateJarCall);
    return Call.tree("Compile " + suffix, javacCall, Call.tree("Archive " + suffix, jarCalls));
  }

  public JavacCall generateJavacCall(List<String> modules) {
    var main = project.mainModules();
    return new JavacCall()
        .ifPresent(main.release(), (call, release) -> call.with("--release", release.feature()))
        .with("--module", String.join(",", modules));
  }

  public JarCall generateJarCall(ModuleDescriptor module) {
    var version = module.version().orElse(project.version().value());
    var file = module.name() + "@" + version + ".jar";
    return new JarCall()
        .with("--create")
        .with("--file", file)
        .with("--module-version", version)
        .ifPresent(module.mainClass(), (call, name) -> call.with("--main-class", name));
  }
}
