import static com.github.sormuras.bach.ProjectInfo.Externals.Name.*;

import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.External;
import com.github.sormuras.bach.ProjectInfo.Externals;
import com.github.sormuras.bach.ProjectInfo.Metadata;
import com.github.sormuras.bach.ProjectInfo.Metadata.Checksum;
import com.github.sormuras.bach.ProjectInfo.Tweak;
import com.github.sormuras.bach.project.JavaStyle;

@ProjectInfo(
    name = "bach",
    version = "17-ea",
    format = JavaStyle.GOOGLE,
    compileModulesForJavaRelease = 16,
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
    },
    testModules = "*/test/{java,java-module}",
    testTweaks = {
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
    },
    requires = {"org.junit.platform.console", "org.junit.jupiter", "net.bytebuddy"},
    lookupExternal = {
      @External(module = "junit", via = "junit:junit:4.13.2"),
      @External(module = "org.hamcrest", via = "org.hamcrest:hamcrest:2.2"),
    },
    lookupExternals = {
      @Externals(name = JAVAFX, version = "15.0.1"),
      @Externals(name = JUNIT, version = "5.8.0-M1"),
      @Externals(name = LWJGL, version = "3.2.3"),
      @Externals(name = SORMURAS_MODULES, version = "2021.03"),
      @Externals(name = GITHUB_RELEASES, version = "*"),
    },
    metadata = {
      @Metadata(
          module = "net.bytebuddy",
          size = 3502105,
          checksums = @Checksum("5c3f1e9eca0d4a71fdf47ddf9311a4c4")),
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
    })
module bach.info {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.Bach.Provider with
      bach.info.CustomBach;
}
