package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.Options;
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
import com.github.sormuras.bach.api.external.JUnit;
import java.lang.module.ModuleDescriptor;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class BachTests {

  static final String NAME = "bach";

  private Project expectedProject() {
    var version = ModuleDescriptor.Version.parse("17-ea");
    var folders = Folders.of(".");
    var modulePath = folders.root("com.github.sormuras.bach");
    var main =
        new CodeSpaceMain(
            DeclaredModuleFinder.of(
                new DeclaredModule(
                    modulePath,
                    DeclaredModuleReference.of(modulePath.resolve("main/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(modulePath.resolve("main/java"))),
                    SourceFolders.of())),
            ModulePaths.of(folders.externals()),
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
                    SourceFolders.of()),
                new DeclaredModule(
                    testBase,
                    DeclaredModuleReference.of(testBase.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(testBase.resolve("test/java"))),
                    SourceFolders.of()),
                new DeclaredModule(
                    testIntegration,
                    DeclaredModuleReference.of(
                        testIntegration.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(testIntegration.resolve("test/java"))),
                    SourceFolders.of()),
                new DeclaredModule(
                    testProjects,
                    DeclaredModuleReference.of(testProjects.resolve("test/java/module-info.java")),
                    SourceFolders.of(SourceFolder.of(testProjects.resolve("test/java"))),
                    SourceFolders.of())),
            ModulePaths.of(folders.modules(CodeSpace.MAIN), folders.externals()));
    var spaces = Spaces.of(main, test);
    var tools =
        new Tools(
            Set.of(),
            Set.of("jlink"),
            Tweaks.of(
                new Tweak(EnumSet.allOf(CodeSpace.class), "javac", List.of("-encoding", "UTF-8")),
                new Tweak(EnumSet.allOf(CodeSpace.class), "javac", List.of("-Xlint")),
                new Tweak(Set.of(CodeSpace.MAIN), "javac", List.of("-Werror")),
                new Tweak(EnumSet.allOf(CodeSpace.class), "javadoc", List.of("-encoding", "UTF-8")),
                new Tweak(EnumSet.allOf(CodeSpace.class), "javadoc", List.of("-notimestamp")),
                new Tweak(EnumSet.allOf(CodeSpace.class), "javadoc", List.of("-Xdoclint:-missing")),
                new Tweak(EnumSet.allOf(CodeSpace.class), "javadoc", List.of("-Werror")),
                new Tweak(EnumSet.allOf(CodeSpace.class), "junit", List.of(
                    "--config",
                    "junit.jupiter.execution.parallel.enabled=true"
                )),
                new Tweak(EnumSet.allOf(CodeSpace.class), "junit", List.of(
                    "--config",
                    "junit.jupiter.execution.parallel.mode.default=concurrent"
                ))
            ));
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
    return new Project(NAME, version, folders, spaces, tools, externals);
  }

  @Test
  void build() {
    Assumptions.assumeTrue(getClass().getClassLoader() == ClassLoader.getSystemClassLoader());

    var bach = Bach.of(Logbook.ofErrorPrinter(), Options.of().with("--dry-run", true));

    var actual = bach.project();
    var expected = expectedProject();
    assertEquals(expected, actual);
    assertEquals(0, bach.run(), bach.logbook().toString());
  }
}
