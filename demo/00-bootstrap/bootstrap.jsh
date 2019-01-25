//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/*
 * Open and source "Bach.java" into this jshell session.
 */
/open https://github.com/sormuras/bach/raw/master/src/bach/Bach.java

/*
 * Use it!
 */
var code = new Bach().run("java", "--version")

/exit code
