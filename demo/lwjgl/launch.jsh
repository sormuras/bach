//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/open ../../src/main/Bach.java
var bach = new Bach(List.of("build"))
var code = bach.run()
/exit code
