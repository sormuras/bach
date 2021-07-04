package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Workflow;
import com.github.sormuras.bach.call.CreateDirectoriesCall;
import com.github.sormuras.bach.call.JarCall;
import com.github.sormuras.bach.call.JavacCall;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;

public class CompileTestModulesWorkflow extends Workflow {

  public CompileTestModulesWorkflow(Bach bach) {
    super(bach);
  }

  @Override
  public void execute() {
    bach.execute(generateCallTree());
  }

  public Call.Tree generateCallTree() {
    var test = bach.project().testModules();
    if (test.set().isEmpty()) return Call.tree("No test module present");
    return generateCallTree(DeclaredModules.of(test.set()));
  }

  public Call.Tree generateCallTree(DeclaredModules modules) {
    var feature = Runtime.version().feature();
    var classes = bach.folders().workspace("classes-test-" + feature);
    var destination = bach.folders().workspace("modules-test");

    var size = modules.descriptors().count();
    var suffix = "%d test module%s".formatted(size, size == 1 ? "" : "s");
    return Call.tree(
        "Compile " + suffix,
        generateJavacCall(modules.names().toList(), classes),
        Call.tree("Create test archive directory", new CreateDirectoriesCall(destination)),
        Call.tree(
            "Archive " + suffix,
            modules
                .descriptors()
                .parallel()
                .map(module -> generateJarCall(module, classes, destination))));
  }

  public JavacCall generateJavacCall(List<String> modules, Path classes) {
    var moduleSourcePaths = project.testModules().moduleSourcePaths();
    return new JavacCall()
        .withModule(modules)
        .ifPresent(moduleSourcePaths.patterns(), JavacCall::withModuleSourcePath)
        .ifPresent(moduleSourcePaths.specifics(), JavacCall::withModuleSourcePaths)
        .withEncoding(settings.sourceSettings().encoding())
        .withDirectoryForClasses(classes);
  }

  public JarCall generateJarCall(ModuleDescriptor module, Path classes, Path destination) {
    var name = module.name();
    var version = module.version().orElse(project.version().value());
    var file = destination.resolve(name + "@" + version + "-test.jar");
    return new JarCall()
        .with("--create")
        .with("--file", file)
        .with("--module-version", version + "-test")
        .ifPresent(module.mainClass(), (call, main) -> call.with("--main-class", main))
        .with("-C", classes.resolve(name), ".");
  }
}
