package test.projects;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Factory;
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
import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.api.SourceFolders;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tools;
import org.junit.jupiter.api.Test;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JigsawQuickStartWorldTests {

  static final String NAME = "JigsawQuickStartWorld";

  private Project expectedProject() {
    var version = ModuleDescriptor.Version.parse("0");
    var folders = Folders.of(Path.of("test.projects", NAME));
    var greetings = folders.root("com.greetings");
    var astro = folders.root("org.astro");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    greetings,
                    DeclaredModuleReference.of(greetings.resolve("module-info.java")),
                    SourceFolders.of(SourceFolder.of(greetings)),
                    SourceFolders.of()),
                new DeclaredModule(
                    astro,
                    DeclaredModuleReference.of(astro.resolve("module-info.java")),
                    SourceFolders.of(SourceFolder.of(astro)),
                    SourceFolders.of())),
            ModulePaths.of(folders.externalModules()),
            0);
    var test =
        new CodeSpaceTest(
            DeclaredModuleFinder.of(),
            ModulePaths.of(folders.modules(CodeSpace.MAIN), folders.externalModules()));
    var spaces = Spaces.of(main, test);
    var externals = Externals.of();
    var tools = Tools.of("javac", "jar");
    return new Project(NAME, version, folders, spaces, tools, externals);
  }

  @Test
  void build() {
    var project = expectedProject();
    var core = new Core(Logbook.ofErrorPrinter(), Options.ofDefaultValues(), new Factory(), project.folders());
    var settings = Settings.of();
    var bach = new Bach(core, settings, project);
    assertDoesNotThrow(bach::buildAndWriteLogbook, () -> bach.logbook().toString());
  }
}
