//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open src/main/java/Bach.java
/open src/main/java/Build.java

try {
  Build.main();
} catch (Throwable throwable) {
  throwable.printStackTrace();
  System.exit(1);
}

/exit
