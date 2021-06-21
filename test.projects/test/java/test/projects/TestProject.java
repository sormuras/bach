package test.projects;

import com.github.sormuras.bach.Bach;
import java.nio.file.Path;
import java.util.Optional;

record TestProject(Path root) {

  static TestProject of(String name) {
    return new TestProject(Path.of("test.projects", name));
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
    return new ProcessBuilder(
            "java",
            "--module-path",
            TestProject.findModuleLocation(Bach.class).orElseThrow().toString(),
            "--add-modules",
            "ALL-MODULE-PATH",
            ".bach/src/build.java",
            "--verbose")
        .directory(root.toFile())
        .redirectOutput(root.resolve("build-out.log").toFile())
        .redirectError(root.resolve("build-err.log").toFile())
        .start();
  }
}
