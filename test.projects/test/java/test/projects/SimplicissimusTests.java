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
import com.github.sormuras.bach.api.Option;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.SourceFolders;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tweaks;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class SimplicissimusTests {

  private static Project expectedProject() {
    var name = "Simplicissimus";
    var version = ModuleDescriptor.Version.parse("123");
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
            9,
            Tweaks.of());
    var test =
        new CodeSpaceTest(
            DeclaredModuleFinder.of(),
            ModulePaths.of(folders.modules(CodeSpace.MAIN), folders.externals()),
            Tweaks.of());
    var spaces = Spaces.of(main, test);
    var externals = Externals.of();
    return new Project(name, version, folders, spaces, externals);
  }

  @Test
  void build() throws Exception {
    var name = "Simplicissimus";
    var root = Path.of("test.projects", name);
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of(name + " Options")
                .with(Option.CHROOT, root)
                .with(Option.VERBOSE)
                .with(Option.PROJECT_VERSION, "123")
                .with(Option.MAIN_JAVA_RELEASE, 9)
                .with(Option.MAIN_JAR_WITH_SOURCES)
                .with(Action.BUILD));

    assertEquals(expectedProject(), bach.project());

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
