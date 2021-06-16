package test.projects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Settings;
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
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import test.base.ToolProviders;

class SimplicissimusTests {

  private static Project project(String name, Version version, Folders folders) {
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    folders.root(),
                    DeclaredModuleReference.of(folders.root("module-info.java")),
                    SourceFolders.of(),
                    SourceFolders.of())),
            ModulePaths.of(folders.externalModules()),
            9);
    var spaces = Spaces.of(main, CodeSpaceTest.empty());
    var externals = Externals.of();
    var tools = Tools.of("javac", "jar");
    return new Project(name, version, folders, spaces, tools, externals);
  }

  @Test
  void build() {
    var name = "Simplicissimus";
    var version = Version.parse("123");
    var folders = Folders.of(Path.of("test.projects", name));
    var project = project(name, version, folders);

    var options =
        Options.ofDefaultValues().with("--verbose", "true").with("--main-jar-with-sources", "true");
    var logbook = Logbook.ofErrorPrinter();
    var settings = Settings.of(options, logbook).with(folders);
    var bach = new Bach(settings, project);

    assertDoesNotThrow(bach::buildAndWriteLogbook, () -> bach.logbook().toString());

    assertLinesMatch(
        """
        >> BACH'S INITIALIZATION >>
        Work on project Simplicissimus 123
        >> BUILD >>
        Bach run took .+
        Logbook written to .+
        """
            .lines(),
        bach.logbook().lines());

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
