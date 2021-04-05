import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.Checksum;
import com.github.sormuras.bach.ProjectInfo.ExternalLibrary;
import com.github.sormuras.bach.ProjectInfo.ExternalModule;
import com.github.sormuras.bach.ProjectInfo.Libraries;
import com.github.sormuras.bach.ProjectInfo.LibraryName;
import com.github.sormuras.bach.ProjectInfo.MainSpace;
import com.github.sormuras.bach.ProjectInfo.Metadata;
import com.github.sormuras.bach.ProjectInfo.Options;
import com.github.sormuras.bach.ProjectInfo.TestSpace;
import com.github.sormuras.bach.ProjectInfo.Tools;
import com.github.sormuras.bach.ProjectInfo.Tweak;
import com.github.sormuras.bach.project.CodeStyle;

@ProjectInfo(
    name = "bach",
    version = "17-ea",
    // <editor-fold desc="Options">
    options =
        @Options(
            formatSourceFilesWithCodeStyle = CodeStyle.GOOGLE,
            compileModulesForJavaRelease = 16,
            includeSourceFilesIntoModules = true,
            tools = @Tools(skip = "jlink")),
    // </editor-fold>
    // <editor-fold desc="Main Space">
    main =
        @MainSpace(
            modules = "*/main/java",
            tweaks = {
              @Tweak(tool = "javac", option = "-encoding", value = "UTF-8"),
              @Tweak(tool = "javac", option = "-g"),
              @Tweak(tool = "javac", option = "-parameters"),
              @Tweak(tool = "javac", option = "-Xlint"),
              @Tweak(tool = "javac", option = "-Werror"),
              @Tweak(tool = "javadoc", option = "-encoding", value = "UTF-8"),
              @Tweak(tool = "javadoc", option = "-notimestamp"),
              @Tweak(tool = "javadoc", option = "-Xdoclint:-missing"),
              @Tweak(tool = "javadoc", option = "-Werror"),
            }),
    // </editor-fold>
    // <editor-fold desc="Test Space">
    test =
        @TestSpace(
            modules = "*/test/{java,java-module}",
            tweaks = {
              @Tweak(tool = "javac", option = "-encoding", value = "UTF-8"),
              @Tweak(tool = "junit", option = "--fail-if-no-tests"),
              @Tweak(
                  tool = "junit(test.projects)",
                  option = "--config",
                  value = "junit.jupiter.execution.parallel.enabled=true"),
              @Tweak(
                  tool = "junit(test.projects)",
                  option = "--config",
                  value = "junit.jupiter.execution.parallel.mode.default=concurrent"),
            }),
    // </editor-fold>
    // <editor-fold desc="Libraries">
    libraries =
        @Libraries(
            requires = "org.junit.platform.console",
            // <editor-fold desc="Libraries - Module Lookup">
            externalModules = {
              @ExternalModule(named = "junit", via = "junit:junit:4.13.2"),
              @ExternalModule(named = "org.hamcrest", via = "org.hamcrest:hamcrest:2.2"),
            },
            externalLibraries = {
              @ExternalLibrary(named = LibraryName.JAVAFX, version = "16"),
              @ExternalLibrary(named = LibraryName.JUNIT, version = "5.8.0-M1"),
              @ExternalLibrary(named = LibraryName.LWJGL, version = "3.2.3"),
              @ExternalLibrary(named = LibraryName.SORMURAS_MODULES, version = "2021.04.01"),
              @ExternalLibrary(named = LibraryName.GITHUB_RELEASES, version = "*"),
            },
            // </editor-fold>
            // <editor-fold desc="Libraries - Metadata">
            metadata = {
              @Metadata(
                  module = "org.apiguardian.api",
                  size = 6452,
                  checksums = @Checksum("6d7c20e025e5ebbaca430f61be707579")),
              @Metadata(
                  module = "org.junit.jupiter",
                  size = 6366,
                  checksums = @Checksum("1c1e0d2ce109b539da3fecc0a97f6201")),
              @Metadata(
                  module = "org.junit.jupiter.api",
                  size = 187629,
                  checksums = @Checksum("a894e975ecfd352e9cf0f980d9017539")),
              @Metadata(
                  module = "org.junit.jupiter.engine",
                  size = 222297,
                  checksums = @Checksum("199a1806027f522125ca5bd680a3fe52")),
              @Metadata(
                  module = "org.junit.jupiter.params",
                  size = 569947,
                  checksums = @Checksum("0cbf90c01777ec3ad941c212f5fad201")),
              @Metadata(
                  module = "org.junit.platform.commons",
                  size = 100503,
                  checksums = @Checksum("6b9a034f45c5ea0986cefa5dae853f36")),
              @Metadata(
                  module = "org.junit.platform.console",
                  size = 488059,
                  checksums = @Checksum("dabf0ba89f8aab1c2087016696506bc4")),
              @Metadata(
                  module = "org.junit.platform.engine",
                  size = 185133,
                  checksums = @Checksum("6263f2f2789c30511fdc32d17cd2b5c9")),
              @Metadata(
                  module = "org.junit.platform.launcher",
                  size = 153701,
                  checksums = @Checksum("ea20a6d9686dc047fb46b303c78b22bb")),
              @Metadata(
                  module = "org.junit.platform.reporting",
                  size = 26149,
                  checksums = @Checksum("503dc5eee8d348f424e3efac1ea95941")),
              @Metadata(
                  module = "org.opentest4j",
                  size = 7653,
                  checksums = @Checksum("45c9a837c21f68e8c93e85b121e2fb90")),
            }
            // </editor-fold>
            )
    // </editor-fold>
    )
module bach.info {
  requires com.github.sormuras.bach;
  requires java.desktop;

  provides com.github.sormuras.bach.Bach.Provider with
      bach.info.CustomBach;
  provides com.github.sormuras.bach.Bach.OnTestsSuccessful with
      bach.info.Teal,
      bach.info.Green;
}
