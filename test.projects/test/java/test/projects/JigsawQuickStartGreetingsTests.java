package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.api.SourceFolders;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tools;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JigsawQuickStartGreetingsTests {

  static final String NAME = "JigsawQuickStartGreetings";

  private Project expectedProject() {
    var version = ModuleDescriptor.Version.parse("0");
    var folders = Folders.of(Path.of("test.projects", NAME));
    var greetings = folders.root("com.greetings");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    greetings,
                    DeclaredModuleReference.of(greetings.resolve("module-info.java")),
                    SourceFolders.of(SourceFolder.of(greetings)),
                    SourceFolders.of())),
            ModulePaths.of(folders.externals()),
            0);
    var test =
        new CodeSpaceTest(
            DeclaredModuleFinder.of(),
            ModulePaths.of(folders.modules(CodeSpace.MAIN), folders.externals()));
    var spaces = Spaces.of(main, test);
    var tools = Tools.of();
    var externals = Externals.of();
    return new Project(NAME, version, folders, spaces, tools, externals);
  }

  @Test
  void build() {
    var root = Path.of("test.projects", NAME);
    var bach =
        Bach.of(
            Logbook.ofErrorPrinter(),
            Options.of()
                .id(NAME + " Options")
                .with("chroot", Optional.of(root))
                .with("verbose", true)
                .with("actions", List.of(Action.BUILD)));

    assertEquals(expectedProject(), bach.project());
    assertEquals(0, bach.run(), bach.logbook().toString());
  }
}
