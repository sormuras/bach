// Bach's Initialization Script

System.out.println(
"""
    ___      ___      ___      ___
   /\\  \\    /\\  \\    /\\  \\    /\\__\\
  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_
 /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\
 \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /
  \\::/  /   /:/  /  \\:\\__\\    /:/  /
   \\/__/    \\/__/    \\/__/    \\/__/.java

        Java Runtime %s
    Operating System %s
   Working Directory %s
"""
.formatted(
  Runtime.version(),
  System.getProperty("os.name"),
  Path.of("").toAbsolutePath()
))

void pwd() { System.out.println(Path.of("").toAbsolutePath()); }

/open https://github.com/sormuras/bach/raw/main/.bach/source/Bach.java

void dir() { Bach.dir(); }
void tree() { Bach.tree(); }
Bach.Project project() { return new Bach.Project(); }
