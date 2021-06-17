package test.projects;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.tool.JavaCall;
import java.nio.file.Path;
import java.util.Optional;

record TestProject(Folders folders) {

  static TestProject of(String name) {
    return new TestProject(Folders.of(Path.of("test.projects", name)));
  }

  static Optional<Path> findModuleLocation(Class<?> type) throws Exception {
    var module = type.getModule();
    if (module.isNamed()) {
      var configuration = module.getLayer().configuration();
      var resolvedModule = configuration.findModule(module.getName()).orElseThrow();
      return resolvedModule.reference().location().map(Path::of);
    }
    var code = type.getProtectionDomain().getCodeSource();
    if (code == null) return Optional.empty();
    var location = code.getLocation();
    if (location == null) return Optional.empty();
    return Optional.of(Path.of(location.toURI()));
  }

  Process build() throws Exception {
    var java =
        new JavaCall()
            .with("java")
            .with("--module-path", TestProject.findModuleLocation(Bach.class).orElseThrow())
            .with("--add-modules", "ALL-MODULE-PATH")
            .with(".bach/src/build.java")
            .with("--verbose");
    return new ProcessBuilder(java.arguments())
        .directory(folders.root().toFile())
        .redirectOutput(folders.root("build-out.log").toFile())
        .redirectError(folders.root("build-err.log").toFile())
        .start();
  }
}
