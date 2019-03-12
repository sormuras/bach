//usr/bin/env jshell --show-version "$0" "$@"; exit $?

/open ../../src/bach/Bach.java

var bach = new Bach()
var code = 0
try {
  bach.build();
  bach.launch();
} catch (Throwable throwable) {
  throwable.printStackTrace();
  code = 1;
}

/exit code
