//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open src/bach/Bach.java
/open src/bach/Command.java
/open src/bach/JdkTool.java
/open src/bach/Bach.jsh

java("--version")

/open src/build/Build.java

Build.main()

/exit
