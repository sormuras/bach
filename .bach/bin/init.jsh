// Bach's Init Script

System.out.println(
"""
    ___      ___      ___      ___
   /\\  \\    /\\  \\    /\\  \\    /\\__\\
  /::\\  \\  /::\\  \\  /::\\  \\  /:/__/_
 /::\\:\\__\\/::\\:\\__\\/:/\\:\\__\\/::\\/\\__\\
 \\:\\::/  /\\/\\::/  /\\:\\ \\/__/\\/\\::/  /
  \\::/  /   /:/  /  \\:\\__\\    /:/  /
   \\/__/    \\/__/    \\/__/    \\/__/.init

        Java Runtime %s
    Operating System %s
   Working Directory %s
"""
.formatted(
  Runtime.version(),
  System.getProperty("os.name"),
  Path.of("").toAbsolutePath()
))

/open https://github.com/sormuras/bach/raw/main/.bach/bin/init.java

init.main()

/exit
