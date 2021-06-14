package test.projects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
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
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JigsawQuickStartGreetingsTests {

  private Project project() {
    var name = "JigsawQuickStartGreetings";
    var version = ModuleDescriptor.Version.parse("0");
    var folders = Folders.of(Path.of("test.projects", name));
    var greetings = folders.root("com.greetings");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    greetings,
                    DeclaredModuleReference.of(greetings.resolve("module-info.java")),
                    SourceFolders.of(SourceFolder.of(greetings)),
                    SourceFolders.of())),
            ModulePaths.of(folders.externalModules()),
            0);
    var spaces = Spaces.of(main, CodeSpaceTest.empty());
    var tools = Tools.of("javac", "jar");
    var externals = Externals.of();
    return new Project(name, version, folders, spaces, tools, externals);
  }

  @Test
  void build() {
    var project = project();
    var core = new Core(Logbook.ofErrorPrinter(), Options.ofDefaultValues(), new Factory(), project.folders());
    var bach = new Bach(core, project);
    assertDoesNotThrow(bach::build, () -> bach.logbook().toString());
  }
}
