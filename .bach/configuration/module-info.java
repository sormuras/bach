import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.External;

@ProjectInfo(
    name = "bach",
    version = "17-ea",
    requires = "org.junit.platform.console",
    lookup = {
        @External(module = "junit", via = "junit:junit:4.13.1"),
        @External(module = "org.hamcrest", via = "org.hamcrest:hamcrest:2.2"),
    }
)
module configuration {
  requires com.github.sormuras.bach;
  provides com.github.sormuras.bach.Bach.Factory with configuration.Modulation;
}
