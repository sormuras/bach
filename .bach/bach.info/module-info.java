import com.github.sormuras.bach.api.*;
import com.github.sormuras.bach.api.ProjectInfo.*;

@ProjectInfo(
    name = "bach",
    // version = "17-ea",
    // <editor-fold desc="Options">
    options = @Options(flags = Option.VERBOSE, actions = Action.BUILD)
    // </editor-fold>
    )
module bach.info {
  requires com.github.sormuras.bach;
}
