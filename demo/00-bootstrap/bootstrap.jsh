//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/*
 * Open and source "Bach.java" and "Bach.jsh" into this jshell session.
 */
/open https://github.com/sormuras/bach/raw/master/src/bach/Bach.java
/open https://github.com/sormuras/bach/raw/master/src/bach/Bach.jsh

/*
 * Use it!
 */
var code = java("--version")

/exit code
