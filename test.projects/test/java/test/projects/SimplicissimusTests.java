package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.DeclaredModuleFinder;
import com.github.sormuras.bach.api.DeclaredModuleReference;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.ModulePaths;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.SourceFolders;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tools;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class SimplicissimusTests {

  private static Project expectedProject() {
    var name = "Simplicissimus";
    var version = Version.parse("123");
    var folders = Folders.of(Path.of("test.projects", name));
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    folders.root(),
                    DeclaredModuleReference.of(folders.root("module-info.java")),
                    SourceFolders.of(),
                    SourceFolders.of())),
            ModulePaths.of(folders.externals()),
            9);
    var test =
        new CodeSpaceTest(
            DeclaredModuleFinder.of(),
            ModulePaths.of(folders.modules(CodeSpace.MAIN), folders.externals()));
    var spaces = Spaces.of(main, test);
    var externals = Externals.of();
    var tools = Tools.of();
    return new Project(name, version, folders, spaces, tools, externals);
  }

  @Test
  void build() throws Exception {
    var name = "Simplicissimus";
    var root = Path.of("test.projects", name);
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of()
                .id(name + " Options")
                .with("chroot", root)
                .with("verbose", true)
                .with("projectVersion", Version.parse("123"))
                .with("mainJavaRelease", 9)
                .with("mainJarWithSources", true)
                .with("actions", Action.BUILD));

    var expectedProject = expectedProject();
    assertEquals(expectedProject.name(), bach.project().name());
    assertEquals(expectedProject.version(), bach.project().version());
    assertEquals(expectedProject.folders(), bach.project().folders());
    assertEquals(expectedProject.spaces(), bach.project().spaces());
    assertEquals(expectedProject.tools(), bach.project().tools());
    assertEquals(expectedProject.externals(), bach.project().externals());
    assertEquals(expectedProject, bach.project());

    var cli =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.ofCommandLineArguments(
                """
                --chroot
                  %s
                --verbose
                --project-version
                  123
                --main-java-release
                  9
                --main-jar-with-sources
                build
                """.formatted(root)));

    assertEquals(expectedProject, cli.project());

    assertEquals(0, bach.run(), bach.logbook().toString());
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Simplicissimus 0
        run(BUILD)
        >> BUILD >>
        Bach run took .+
        Logbook written to .+
        """
            .lines(),
        bach.logbook().lines());

    var folders = bach.project().folders();
    var jar = folders.modules(CodeSpace.MAIN, "simplicissimus@0.jar");

    var classes = folders.workspace("classes");
    ToolProviders.run("javac", folders.root("module-info.java"), "-d", classes);
    Files.createDirectories(jar.getParent());
    ToolProviders.run("jar", "--create", "--file", jar, "-C", classes, ".");

    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        module-info.class
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
