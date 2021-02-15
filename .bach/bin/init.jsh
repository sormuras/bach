// Bach's Init Script

System.out.printf(
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
""",
  Runtime.version(),
  System.getProperty("os.name"),
  Path.of("").toAbsolutePath())

/open https://github.com/sormuras/bach/raw/main/.bach/bin/init.java

var status = 0
try {
  init.main();
  System.out.print(
      """

      Browse https://github.com/sormuras/bach for Bach's code, documentation,
      discussions, issues, sponsoring, and more. Have fun!

      """);
}
catch(Exception exception) {
  System.err.println(exception.getMessage());
  status = 1;
}

/exit status
