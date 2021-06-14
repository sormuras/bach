import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Core;
import com.github.sormuras.bach.Factory;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.CodeSpaceMain;
import com.github.sormuras.bach.api.CodeSpaceTest;
import com.github.sormuras.bach.api.DeclaredModule;
import com.github.sormuras.bach.api.DeclaredModuleFinder;
import com.github.sormuras.bach.api.DeclaredModuleReference;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.ExternalModuleLocations;
import com.github.sormuras.bach.api.Externals;
import com.github.sormuras.bach.api.Folders;
import com.github.sormuras.bach.api.ModulePaths;
import com.github.sormuras.bach.api.Project;
import com.github.sormuras.bach.api.SourceFolder;
import com.github.sormuras.bach.api.SourceFolders;
import com.github.sormuras.bach.api.Spaces;
import com.github.sormuras.bach.api.Tools;
import com.github.sormuras.bach.api.Tweak;
import com.github.sormuras.bach.api.Tweaks;
import com.github.sormuras.bach.locator.JUnit;
import com.github.sormuras.bach.workflow.ExecuteTestsWorkflow;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

public class build {
  public static void main(String... args) {
    System.out.println("Build with Bach " + Bach.version());
    try {
      bach(args).buildAndWriteLogbook();
    } catch (Throwable throwable) {
      System.exit(-1);
    }
  }

  static Bach bach(String... args) {
    var options = Options.ofCommandLineArguments(args).underlay(Options.ofDefaultValues());
    return new Bach(core(options), project(options));
  }

  static Core core(Options options) {
    var logbook = Logbook.of(Printer.ofSystem(), options.verbose());
    var factory = new MyFactory();
    var folders = Folders.of("");
    return new Core(logbook, options, factory, folders);
  }

  static Project project(Options options) {
    var version = options.project_version();
    var folders = Folders.of(".");
    var modulePath = folders.root("com.github.sormuras.bach");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    modulePath,
                    DeclaredModuleReference.of(modulePath.resolve("main/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(modulePath.resolve("main/java"))),
                    SourceFolders.of(SourceFolder.of(modulePath.resolve("main/java"))))),
            ModulePaths.of(folders.externalModules()),
            0);
    var testBase = folders.root("test.base");
    var testIntegration = folders.root("test.integration");
    var testProjects = folders.root("test.projects");
    var test =
        new CodeSpaceTest(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    modulePath,
                    DeclaredModuleReference.of(
                        modulePath.resolve("test/java-module/module-info.java")),
                    SourceFolders.of(
                        SourceFolder.of(modulePath.resolve("test/java")),
                        SourceFolder.of(modulePath.resolve("test/java-module"))),
                    SourceFolders.of(
                        SourceFolder.of(modulePath.resolve("test/java")),
                        SourceFolder.of(modulePath.resolve("test/java-module")))),
                new DeclaredModule(
                    testBase,
                    DeclaredModuleReference.of(testBase.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(testBase.resolve("test/java"))),
                    SourceFolders.of(SourceFolder.of(testBase.resolve("test/java")))),
                new DeclaredModule(
                    testIntegration,
                    DeclaredModuleReference.of(
                        testIntegration.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(testIntegration.resolve("test/java"))),
                    SourceFolders.of(SourceFolder.of(testIntegration.resolve("test/java")))),
                new DeclaredModule(
                    testProjects,
                    DeclaredModuleReference.of(testProjects.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(testProjects.resolve("test/java"))),
                    SourceFolders.of(
                        SourceFolder.of(testProjects.resolve("test/java")),
                        SourceFolder.of(testProjects.resolve("test/resources"))))),
            ModulePaths.of(folders.modules(CodeSpace.MAIN), folders.externalModules()));
    var spaces = Spaces.of(main, test);
    var tools =
        new Tools(
            Set.of(),
            Set.of("jlink"),
            Tweaks.of(
                new Tweak(
                    EnumSet.allOf(CodeSpace.class),
                    "javac",
                    List.of("-encoding", "UTF-8", "-Xlint")),
                new Tweak(Set.of(CodeSpace.MAIN), "javac", List.of("-Werror")),
                new Tweak(
                    EnumSet.allOf(CodeSpace.class),
                    "javadoc",
                    List.of("-encoding", "UTF-8", "-notimestamp", "-Xdoclint:-missing", "-Werror")),
                new Tweak(
                    EnumSet.allOf(CodeSpace.class),
                    "jlink",
                    List.of("--launcher", "bach=com.github.sormuras.bach")),
                new Tweak(
                    EnumSet.allOf(CodeSpace.class),
                    "junit",
                    List.of(
                        "--config", "junit.jupiter.execution.parallel.enabled=true",
                        "--config", "junit.jupiter.execution.parallel.mode.default=concurrent"))));
    var externals =
        new Externals(
            Set.of("org.junit.platform.console"),
            List.of(
                ExternalModuleLocations.of(
                    new ExternalModuleLocation(
                        "junit",
                        "https://repo.maven.apache.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar"),
                    new ExternalModuleLocation(
                        "org.hamcrest",
                        "https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest/2.2/hamcrest-2.2.jar")),
                JUnit.V_5_8_0_M1));
    return new Project("bach", version, folders, spaces, tools, externals);
  }

  static class MyFactory extends Factory {
    @Override
    public ExecuteTestsWorkflow newExecuteTestsWorkflow(Bach bach) {
      return new MyExecuteTestsWorkflow(bach);
    }
  }

  static class MyExecuteTestsWorkflow extends ExecuteTestsWorkflow {

    public MyExecuteTestsWorkflow(Bach bach) {
      super(bach);
    }

    @Override
    protected ToolRun runJUnit(ToolProvider provider, String module) {
      var project = bach().project();
      var tweaks = project.tools().tweaks();
      var java =
          new MyJavaCall(List.of())
              .with("--show-version")
              .with("-enableassertions")
              .with("--module-path", computeModulePaths(module))
              .with("--add-modules", module)
              .with("--add-modules", "ALL-DEFAULT")
              .with("--module", "org.junit.platform.console")
              .with("--disable-banner")
              .with("--select-module", module)
              .withAll(tweaks.arguments(CodeSpace.TEST, "junit"))
              .withAll(tweaks.arguments(CodeSpace.TEST, "junit(" + module + ")"))
              .with("--reports-dir", project.folders().workspace("reports", "junit", module));
      return bach().run(java);
    }
  }

  record MyJavaCall(List<String> arguments) implements ToolCall<MyJavaCall>, ToolProvider {

    @Override
    public MyJavaCall arguments(List<String> arguments) {
      return new MyJavaCall(arguments);
    }

    @Override
    public String name() {
      return "java";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      var builder = new ProcessBuilder("java");
      builder.command().addAll(arguments());
      try {
        var process = builder.start();
        new Thread(new StreamGobbler(process.getInputStream(), out::println)).start();
        new Thread(new StreamGobbler(process.getErrorStream(), err::println)).start();
        return process.waitFor();
      } catch (Exception exception) {
        exception.printStackTrace(err);
        return -1;
      }
    }

    record StreamGobbler(InputStream stream, Consumer<String> consumer) implements Runnable {
      public void run() {
        new BufferedReader(new InputStreamReader(stream)).lines().forEach(consumer);
      }
    }
  }
}
