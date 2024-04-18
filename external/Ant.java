/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach.external;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.spi.ToolProvider;
import run.bach.ToolCall;
import run.bach.ToolFinder;
import run.bach.ToolInstaller;
import run.bach.ToolProgram;
import run.bach.ToolSpace;

/**
 * Apache Ant installer.
 *
 * @see <a href="https://ant.apache.org">https://ant.apache.org</a>
 */
public record Ant(String version) implements ToolInstaller {
  public static final String DEFAULT_VERSION = "1.10.14";

  public static void main(String... args) {
    var version = System.getProperty("version", DEFAULT_VERSION);
    new Ant(version).install().run(args.length == 0 ? new String[] {"-version"} : args);
  }

  public Ant() {
    this(DEFAULT_VERSION);
  }

  @Override
  public ToolProvider install(Path into) throws Exception {
    var title = "apache-ant-" + version;
    var archive = title + "-bin.zip";
    var target = into.resolve(archive);
    if (!Files.exists(target)) {
      var source = "https://dlcdn.apache.org/ant/binaries/" + archive;
      download(target, URI.create(source));
    }
    var antHome = into.resolve(title);
    if (!Files.isDirectory(antHome)) {
      var jar =
          ToolProgram.findJavaDevelopmentKitTool("jar")
              .orElseThrow()
              .withProcessBuilderTweaker(builder -> builder.directory(into.toFile()))
              .withProcessWaiter(process -> process.waitFor(1, TimeUnit.MINUTES) ? 0 : 1)
              .tool();
      var silent =
          new ToolSpace(ToolFinder.compose()) {
            @Override
            protected void announce(ToolCall call) {}
          };
      silent.run(jar, "--extract", "--file", archive);
    }
    return ToolProgram.java(
        "--class-path",
        antHome.resolve("lib/ant-launcher.jar").toString(),
        "org.apache.tools.ant.launch.Launcher");
  }
}
