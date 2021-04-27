import com.github.sormuras.bach.api.*;
import com.github.sormuras.bach.api.ProjectInfo.*;

@ProjectInfo(
    name = "bach",
    // version = "17-ea",
    // <editor-fold desc="Command-Line Options">
    options = @Options(flags = Option.VERBOSE, actions = Action.BUILD),
    // </editor-fold>
    // <editor-fold desc="External Modules">
    requires = "org.junit.platform.console",
    external =
        @External(
            // <editor-fold desc="External Module Locators">
            externalModules = {
              @ExternalModule(name = "junit", link = "junit:junit:4.13.2"),
              @ExternalModule(name = "org.hamcrest", link = "org.hamcrest:hamcrest:2.2"),
            },
            externalLibraries = {
              @ExternalLibrary(name = ExternalLibraryName.JUNIT, version = "5.8.0-M1"),
            }
            // </editor-fold>
            )
    // </editor-fold>
    )
module bach.info {
  requires com.github.sormuras.bach;
}
