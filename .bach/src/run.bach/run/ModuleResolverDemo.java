package run;

import bach.info.org.jreleaser.JReleaser;
import bach.info.org.junit.JUnit;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import run.bach.ModuleFinders;
import run.bach.ModuleResolver;
import run.bach.ToolFinder;
import run.bach.workflow.Folders;
import run.external.Ant;

public class ModuleResolverDemo {
  public static void main(String... args) throws Exception {
    var libraries = ModuleFinder.compose(ModuleFinders.ofProperties(JUnit.MODULES));

    try (var reader = libraries.find("org.junit.jupiter").orElseThrow().open()) {
      reader.list().forEach(System.out::println);
    }

    var lib = Path.of("lib");
    var resolver = ModuleResolver.ofSingleDirectory(lib, libraries);
    resolver.resolveModule("org.junit.jupiter"); // to write and discover tests
    resolver.resolveModule("org.junit.platform.console"); // to run tests
    resolver.resolveMissingModules();

    // "jreleaser" via the tool provider SPI
    var jreleaserHome =
        Folders.ofCurrentWorkingDirectory().tool(JReleaser.NAME + "@" + JReleaser.VERSION);
    var jreleaserResolver = ModuleResolver.ofSingleDirectory(jreleaserHome, JReleaser.MODULES);
    jreleaserResolver.resolveModule("org.jreleaser.tool");
    jreleaserResolver.resolveMissingModules();

    var tools =
        ToolFinder.compose(
            ToolFinder.of("jar"), // provides "jar" tool
            ToolFinder.of("java"), // provides "java" tool
            ToolFinder.of(ModuleFinder.of(lib)), // provides "junit" tool
            ToolFinder.of(ModuleFinder.of(jreleaserHome)), // provides "jreleaser" tool
            ToolFinder.ofInstaller()
                .withJavaApplication("demo/release@uri", JReleaser.APPLICATION)
                .withJavaApplication(
                    "demo/release@all", JReleaser.APPLICATION, JReleaser.APPLICATION_ASSETS)
                .with(new Ant()) // provides "ant" tool
            );

    var junit = tools.get("junit");
    junit.run("--version");
    junit.run("engines");

    tools.get("jreleaser").run("--version");
    tools.get("releaser1").run("--version");
    tools.get("releaser2").run("--version");

    tools.get("ant").run("-version");
  }
}
