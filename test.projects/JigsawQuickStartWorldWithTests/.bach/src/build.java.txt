import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
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
import java.lang.module.ModuleDescriptor;

class build {
  public static void main(String... args) {
    var options = Options.ofCommandLineArguments(args).underlay(Options.ofDefaultValues());
    var logbook = Logbook.of(Printer.ofSystem(), options.verbose());
    var bach = new Bach(Settings.of(options, logbook), project());
    bach.buildAndWriteLogbook();
  }

  static Project project() {
    var name = "JigsawQuickStartWorldWithTests";
    var version = ModuleDescriptor.Version.parse("99");
    var folders = Folders.of(".");

    var greetings = folders.root("com.greetings");
    var astro = folders.root("org.astro");
    var tests = folders.root("test.modules");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    greetings,
                    DeclaredModuleReference.of(greetings.resolve("main/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(greetings.resolve("main/java"))),
                    SourceFolders.of()),
                new DeclaredModule(
                    astro,
                    DeclaredModuleReference.of(astro.resolve("main/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(astro.resolve("main/java"))),
                    SourceFolders.of()) //
            ),
            ModulePaths.of(folders.externalModules()),
            0);
    var test =
        new CodeSpaceTest(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    tests,
                    DeclaredModuleReference.of(tests.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(tests.resolve("test/java"))),
                    SourceFolders.of(SourceFolder.of(tests.resolve("test/java")))) //
            ),
            ModulePaths.of(folders.modules(CodeSpace.MAIN), folders.externalModules()));
    var spaces = Spaces.of(main, test);
    var tools = Tools.of("javac", "jar", "test");
    var externals = Externals.of();

    return new Project(name, version, folders, spaces, tools, externals);
  }
}
