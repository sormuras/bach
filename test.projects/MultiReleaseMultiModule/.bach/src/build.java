import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.Settings;
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
    var options =
        Options.ofCommandLineArguments(args)
            .with("--main-jar-with-sources", "true")
            .underlay(Options.ofDefaultValues());
    var logbook = Logbook.of(Printer.ofSystem(), options.verbose());
    var bach = new Bach(Settings.of(options, logbook), project());
    bach.buildAndWriteLogbook();
  }

  static Project project() {
    var name = "MultiReleaseMultiModule";
    var version = ModuleDescriptor.Version.parse("99");
    var folders = Folders.of(".");
    var api = folders.root("api");
    var engine = folders.root("engine");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    api,
                    DeclaredModuleReference.of(api.resolve("java-module/module-info.java")),
                    SourceFolders.of(
                        SourceFolder.of(api.resolve("java")),
                        SourceFolder.of(api.resolve("java-module"))),
                    SourceFolders.of(
                        SourceFolder.of(api.resolve("java")),
                        SourceFolder.of(api.resolve("java-module")))),
                new DeclaredModule(
                    engine,
                    DeclaredModuleReference.of(engine.resolve("java-module/module-info.java")),
                    SourceFolders.of(
                        SourceFolder.of(engine.resolve("java")),
                        SourceFolder.of(engine.resolve("java-module")),
                        SourceFolder.of(engine.resolve("java-11"))),
                    SourceFolders.of(
                        SourceFolder.of(engine.resolve("java")),
                        SourceFolder.of(engine.resolve("java-module")),
                        SourceFolder.of(engine.resolve("java-11"))))),
            ModulePaths.of(folders.externalModules()),
            8);
    var spaces = Spaces.of(main, CodeSpaceTest.empty());
    var tools = Tools.of("javac", "jar");
    var externals = Externals.of();
    return new Project(name, version, folders, spaces, tools, externals);
  }
}
