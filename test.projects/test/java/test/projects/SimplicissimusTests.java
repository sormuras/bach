package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
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
import com.github.sormuras.bach.tool.AnyCall;
import java.lang.module.ModuleDescriptor.Version;
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
    var tools = Tools.of("javac", "jar");
    return new Project(name, version, folders, spaces, tools, externals);
  }

  @Test
  void build() {
    var name = "Simplicissimus";
    var root = Path.of("test.projects", name);
    var args =
        new AnyCall("bach")
            .with("--chroot", root)
            .with("--verbose")
            .with("--limit-tool", "javac")
            .with("--limit-tool", "jar")
            .with("--project-version", "123")
            .with("--main-java-release", 9)
            .with("--main-jar-with-sources")
            .with("build")
            .arguments();
    var bach = Bach.of(Logbook.ofErrorPrinter(), Options.ofCommandLineArguments(args));

    var expectedProject = expectedProject();
    assertEquals(expectedProject.name(), bach.project().name());
    assertEquals(expectedProject.version(), bach.project().version());
    assertEquals(expectedProject.folders(), bach.project().folders());
    assertEquals(expectedProject.spaces(), bach.project().spaces());
    assertEquals(expectedProject.tools(), bach.project().tools());
    assertEquals(expectedProject.externals(), bach.project().externals());
    assertEquals(expectedProject, bach.project());

    assertEquals(0, bach.run(), () -> bach.logbook().toString());
    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Work on project Simplicissimus 123
        run(BUILD)
        >> BUILD >>
        Bach run took .+
        Logbook written to .+
        """
            .lines(),
        bach.logbook().lines());

    var folders = bach.project().folders();
    var jar = folders.modules(CodeSpace.MAIN, "simplicissimus@123.jar");

    assertLinesMatch(
        """
        META-INF/
        META-INF/MANIFEST.MF
        README.md
        module-info.class
        module-info.java
        """
            .lines()
            .sorted(),
        ToolProviders.run("jar", "--list", "--file", jar).lines().sorted());
  }
}
