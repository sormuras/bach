import static com.github.sormuras.bach.ProjectInfo.Externals.Name.*;

import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.External;
import com.github.sormuras.bach.ProjectInfo.Externals;
import com.github.sormuras.bach.project.JavaStyle;

@ProjectInfo(
    name = "bach",
    version = "17-ea",
    format = JavaStyle.GOOGLE,
    requires = {"org.junit.platform.console", "org.junit.jupiter", "net.bytebuddy"},
    lookupExternal = {
      @External(module = "junit", via = "junit:junit:4.13.1"),
      @External(module = "org.hamcrest", via = "org.hamcrest:hamcrest:2.2"),
    },
    lookupExternals = {
      @Externals(name = JAVAFX, version = "15.0.1"),
      @Externals(name = JUNIT, version = "5.8.0-M1"),
      @Externals(name = LWJGL, version = "3.2.3"),
      @Externals(name = SORMURAS_MODULES, version = "2021.02.24"),
      @Externals(name = GITHUB_RELEASES, version = "*"),
    })
module bach.info {
  requires com.github.sormuras.bach;

  provides com.github.sormuras.bach.Bach.Provider with
      bach.info.CustomBach;
}
