package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Workflow;
import com.github.sormuras.bach.call.CreateDirectoriesCall;
import com.github.sormuras.bach.call.JarCall;
import com.github.sormuras.bach.call.JavacCall;
import com.github.sormuras.bach.project.JavaRelease;
import com.github.sormuras.bach.project.ModulePatches;
import com.github.sormuras.bach.project.ModulePaths;
import com.github.sormuras.bach.project.ModuleSourcePaths;
import com.github.sormuras.bach.project.ProjectSpace;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CompileWorkflow extends Workflow {

  protected final ProjectSpace space;

  public CompileWorkflow(Bach bach, ProjectSpace space) {
    super(bach);
    this.space = space;
  }

  @Override
  public void execute() {
    bach.execute(generateCallTree());
  }

  public Call.Tree generateCallTree() {
    if (space.modules().isEmpty()) return Call.tree("No %s module present".formatted(space.name()));
    return generateCallTree(DeclaredModuleFinder.of(space.modules()));
  }

  public Call.Tree generateCallTree(DeclaredModuleFinder finder) {
    var feature = space.release().map(JavaRelease::feature).orElse(Runtime.version().feature());
    var classes = bach.folders().workspace("classes" + space.suffix() + "-" + feature);
    var modules = bach.folders().workspace("modules" + space.suffix());

    var size = finder.descriptors().count();
    return Call.tree(
        "Compile %d %s module%s".formatted(size, space.name(), size == 1 ? "" : "s"),
        generateJavacCall(finder.names().toList(), classes),
        Call.tree("Create main archive directory", new CreateDirectoriesCall(modules)),
        Call.tree(
            "Archive %d %s module%s".formatted(size, space.name(), size == 1 ? "" : "s"),
            finder
                .descriptors()
                .parallel()
                .map(module -> generateJarCall(module, classes, modules))));
  }

  public JavacCall generateJavacCall(List<String> modules, Path classes) {
    var moduleSourcePaths =
        space.moduleSourcePaths().orElseGet(() -> ModuleSourcePaths.of(space.modules()));
    var modulePatches = space.modulePatches().map(ModulePatches::map).orElse(Map.of());
    var modulePaths = space.modulePaths().map(ModulePaths::pruned).orElse(List.of());

    return new JavacCall()
        .ifPresent(space.release(), JavacCall::withRelease)
        .withModule(modules)
        .ifPresent(moduleSourcePaths.patterns(), JavacCall::withModuleSourcePathPatterns)
        .ifPresent(moduleSourcePaths.specifics(), JavacCall::withModuleSourcePathSpecifics)
        .ifPresent(modulePatches, JavacCall::withPatchModules)
        .ifPresent(modulePaths, JavacCall::withModulePath)
        .withEncoding(project.defaults().encoding())
        .withDirectoryForClasses(classes);
  }

  public JarCall generateJarCall(ModuleDescriptor module, Path classes, Path destination) {
    var name = module.name();
    var version = module.version().orElse(project.version().value());
    var file = destination.resolve(name + "@" + version + ".jar");
    var modulePatches =
        space
            .modulePatches()
            .map(ModulePatches::map)
            .orElse(Map.of())
            .getOrDefault(name, List.of());

    return new JarCall()
        .with("--create")
        .with("--file", file)
        .with("--module-version", version + space.suffix())
        .ifPresent(module.mainClass(), (call, main) -> call.with("--main-class", main))
        .with("-C", classes.resolve(name), ".")
        .forEach(modulePatches, (call, path) -> call.with("-C", path, "."));
  }
}
