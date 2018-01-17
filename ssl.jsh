//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open PRINTING

var uri = URI.create("https://oss.sonatype.org/content/repositories/snapshots/org/junit/jupiter/junit-jupiter-api/5.1.0-SNAPSHOT/junit-jupiter-api-5.1.0-20180117.153202-279.jar")
printf("uri: %s%n", uri)

var connection = uri.toURL().openConnection()
printf("size: %d bytes%n", connection.getContentLengthLong())

/exit
