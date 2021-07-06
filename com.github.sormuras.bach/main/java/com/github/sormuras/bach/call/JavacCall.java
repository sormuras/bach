package com.github.sormuras.bach.call;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

public record JavacCall(List<String> arguments) implements AnyCall<JavacCall> {

  public JavacCall() {
    this(List.of());
  }

  @Override
  public String name() {
    return "javac";
  }

  @Override
  public JavacCall arguments(List<String> arguments) {
    if (this.arguments == arguments) return this;
    return new JavacCall(arguments);
  }

  /** @param directory Specify where to place generated class files */
  public JavacCall withDirectoryForClasses(Path directory) {
    return with("-d", directory);
  }

  public JavacCall withEncoding(Charset charset) {
    return with("-encoding", charset);
  }

  public JavacCall withModule(List<String> modules) {
    return with("--module", String.join(",", modules));
  }

  public JavacCall withModulePath(List<Path> paths) {
    return with("--module-path", paths);
  }

  public JavacCall withModuleSourcePathPatterns(List<String> patterns) {
    var joined =
        patterns.stream()
            .map(pattern -> pattern.replace('/', File.separatorChar))
            .map(pattern -> pattern.replace('\\', File.separatorChar))
            .collect(Collectors.joining(File.pathSeparator));
    return with("--module-source-path", joined);
  }

  public JavacCall withModuleSourcePath(String module, List<Path> paths) {
    var joined = paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    return with("--module-source-path", module + '=' + joined);
  }

  public JavacCall withModuleSourcePathSpecifics(Map<String, List<Path>> map) {
    return forEach(map.entrySet(), (c, e) -> c.withModuleSourcePath(e.getKey(), e.getValue()));
  }

  public JavacCall withPatchModule(String module, List<Path> paths) {
    var joined = paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    return with("--patch-module", module + '=' + joined);
  }

  public JavacCall withPatchModules(Map<String, List<Path>> map) {
    return forEach(map.entrySet(), (c, e) -> c.withPatchModule(e.getKey(), e.getValue()));
  }

  public JavacCall withRelease(IntSupplier release) {
    return withRelease(release.getAsInt());
  }

  public JavacCall withRelease(int release) {
    return with("--release", release);
  }
}
