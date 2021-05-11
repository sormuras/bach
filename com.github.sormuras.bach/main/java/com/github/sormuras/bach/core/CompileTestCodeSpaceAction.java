package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.internal.Strings;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javac;
import java.nio.file.Path;
import java.util.ArrayList;

public class CompileTestCodeSpaceAction extends BachAction {

  public CompileTestCodeSpaceAction(Bach bach) {
    super(bach);
  }

  /** Compile and jar test modules. */
  public void compile() {
    var test = bach().project().spaces().test();
    var modules = test.modules();
    if (modules.isEmpty()) {
      bach().log("Test module list is empty, nothing to build here.");
      return;
    }
    var s = modules.size() == 1 ? "" : "s";
    bach().say("Compile %d test module%s: %s".formatted(modules.size(), s, modules.toNames(", ")));

    var feature = Runtime.version().feature();
    var testClasses = bach().project().folders().workspace("classes-test-" + feature);
    var testModules = bach().project().folders().modules(CodeSpace.TEST);

    Paths.deleteDirectories(testModules);
    bach().run(buildTestJavac(testClasses)).requireSuccessful();

    Paths.createDirectories(testModules);
    var jars =
        modules.map().values().stream()
            .map(module -> buildTestJar(testModules, module, testClasses));
    bach().run(jars).requireSuccessful();
  }

  public Javac buildTestJavac(Path classes) {
    var main = bach().project().spaces().main();
    var test = bach().project().spaces().test();
    var tweaks = bach().project().tools().tweaks();
    return new Javac()
        .with("--module", test.modules().toNames(","))
        .forEach(
            test.modules().toModuleSourcePaths(false),
            (javac, path) -> javac.with("--module-source-path", path))
        .ifPresent(test.paths().pruned(), (javac, paths) -> javac.with("--module-path", paths))
        .forEach(
            test.modules().toModulePatches(main.modules()).entrySet(),
            (javac, patch) -> javac.with("--patch-module", patch.getKey() + '=' + patch.getValue()))
        .withAll(tweaks.arguments(CodeSpace.TEST, "javac"))
        .with("-d", classes);
  }

  public Jar buildTestJar(Path testModules, DeclaredModule declared, Path classes) {
    var name = declared.name();
    var file = testModules.resolve(generateJarFileName(name));
    var tweaks = bach().project().tools().tweaks();
    var jar =
        new Jar()
            .ifTrue(bach().options().verbose(), args -> args.with("--verbose"))
            .with("--create")
            .with("--file", file)
            .withAll(tweaks.arguments(CodeSpace.TEST, "jar"))
            .withAll(tweaks.arguments(CodeSpace.TEST, "jar(" + name + ")"));
    var baseClasses = classes.resolve(name);
    jar = jar.with("-C", baseClasses, ".");
    // include base test resources
    for (var folder : declared.resources().list()) {
      if (folder.isTargeted()) continue; // handled later
      jar = jar.with("-C", folder.path(), ".");
    }
    // include targeted test resources in ascending order
    for (int release = 9; release <= Runtime.version().feature(); release++) {
      var paths = new ArrayList<Path>();
      declared.resources().stream(release).map(SourceFolder::path).forEach(paths::add);
      if (paths.isEmpty()) continue;
      jar = jar.with("--release", release);
      for (var path : paths) jar = jar.with("-C", path, ".");
    }
    return jar;
  }

  public String generateJarFileName(String module) {
    return module + '@' + Strings.toNumberAndPreRelease(bach().project().version()) + "+test.jar";
  }
}
