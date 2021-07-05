package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Workflow;
import com.github.sormuras.bach.call.CreateDirectoriesCall;
import com.github.sormuras.bach.call.JarCall;
import com.github.sormuras.bach.call.JavacCall;
import com.github.sormuras.bach.project.JavaRelease;
import com.github.sormuras.bach.project.MainModules;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;

public class CompileMainModulesWorkflow extends Workflow {

  protected final MainModules main;

  public CompileMainModulesWorkflow(Bach bach) {
    super(bach);
    this.main = bach.project().mainModules();
  }

  @Override
  public void execute() {
    bach.execute(generateCallTree());
  }

  public Call.Tree generateCallTree() {
    if (main.set().isEmpty()) return Call.tree("No main module present");
    return generateCallTree(DeclaredModules.of(main.set()));
  }

  public Call.Tree generateCallTree(DeclaredModules modules) {
    var feature = main.release().map(JavaRelease::feature).orElse(Runtime.version().feature());
    var classes = bach.folders().workspace("classes-main-" + feature);
    var destination = bach.folders().workspace("modules");

    var size = modules.descriptors().count();
    return Call.tree(
        "Compile %d main module%s".formatted(size, size == 1 ? "" : "s"),
        generateJavacCall(modules.names().toList(), classes),
        Call.tree("Create main archive directory", new CreateDirectoriesCall(destination)),
        Call.tree(
            "Archive %d main module%s".formatted(size, size == 1 ? "" : "s"),
            modules
                .descriptors()
                .parallel()
                .map(module -> generateJarCall(module, classes, destination))));
  }

  public JavacCall generateJavacCall(List<String> modules, Path classes) {
    return new JavacCall()
        .ifPresent(main.release(), JavacCall::withRelease)
        .withModule(modules)
        .ifPresent(main.moduleSourcePaths().patterns(), JavacCall::withModuleSourcePathPatterns)
        .ifPresent(main.moduleSourcePaths().specifics(), JavacCall::withModuleSourcePathSpecifics)
        .withEncoding(project.defaults().encoding())
        .withDirectoryForClasses(classes);
  }

  public JarCall generateJarCall(ModuleDescriptor module, Path classes, Path destination) {
    var name = module.name();
    var version = module.version().orElse(project.version().value());
    var file = destination.resolve(name + "@" + version + ".jar");
    return new JarCall()
        .with("--create")
        .with("--file", file)
        .with("--module-version", version)
        .ifPresent(module.mainClass(), (call, main) -> call.with("--main-class", main))
        .with("-C", classes.resolve(name), ".");
  }
}
