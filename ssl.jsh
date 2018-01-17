//usr/bin/env jshell --show-version --execution local "$0" "$@"; exit $?

/open PRINTING

var uri = URI.create("https://oss.sonatype.org/content/repositories/snapshots/org/junit/jupiter/junit-jupiter-api/5.1.0-SNAPSHOT/junit-jupiter-api-5.1.0-20180117.153202-279.jar")
printf("uri: %s%n", uri)

var url = uri.toURL()
var connection = url.openConnection()
var length = connection.getContentLength()
printf("length: %d bytes%n", length)

var target = Paths.get("downloaded.jar")
try (var sourceStream = url.openStream(); var targetStream = Files.newOutputStream(target)) {
  sourceStream.transferTo(targetStream);
}
var size = 0
if (Files.exists(target)) size = (int) Files.size(target)
printf("loaded: %d bytes%n", size)

/exit length - size
