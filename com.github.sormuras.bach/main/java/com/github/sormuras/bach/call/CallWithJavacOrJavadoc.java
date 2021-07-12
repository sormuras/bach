package com.github.sormuras.bach.call;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

public interface CallWithJavacOrJavadoc<C extends CallWithJavacOrJavadoc<C>> extends CallWith<C> {

  /** @param directory Specify where to place generated class files */
  default C withDestinationDirectory(Path directory) {
    return with("-d", directory);
  }

  default C withEncoding(Charset charset) {
    return with("-encoding", charset);
  }

  default C withModule(List<String> modules) {
    return with("--module", String.join(",", modules));
  }

  default C withModulePath(List<Path> paths) {
    return with("--module-path", paths);
  }

  default C withModuleSourcePathPatterns(List<String> patterns) {
    var joined =
        patterns.stream()
            .map(pattern -> pattern.replace('/', File.separatorChar))
            .map(pattern -> pattern.replace('\\', File.separatorChar))
            .collect(Collectors.joining(File.pathSeparator));
    return with("--module-source-path", joined);
  }

  default C withModuleSourcePath(String module, List<Path> paths) {
    var joined = paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    return with("--module-source-path", module + '=' + joined);
  }

  default C withModuleSourcePathSpecifics(Map<String, List<Path>> map) {
    return forEach(map.entrySet(), (c, e) -> c.withModuleSourcePath(e.getKey(), e.getValue()));
  }

  default C withPatchModule(String module, List<Path> paths) {
    var joined = paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    return with("--patch-module", module + '=' + joined);
  }

  default C withPatchModules(Map<String, List<Path>> map) {
    return forEach(map.entrySet(), (c, e) -> c.withPatchModule(e.getKey(), e.getValue()));
  }

  default C withRelease(IntSupplier release) {
    return withRelease(release.getAsInt());
  }

  default C withRelease(int release) {
    return with("--release", release);
  }
}