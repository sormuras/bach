import com.github.sormuras.bach.Bach;

class hello {
  public static void main(String... args) throws Exception {
    try (var bach = new Bach()) {
      bach.log("CAPTION:Restore external tool layers");
      var restorer = bach.restorer();
      restorer.restore(
          restorer
              .describeExternalToolLayer("sormuras-hello@1-ea+1")
              .withAsset(
                  "com.github.sormuras.hello@1-ea+1.jar",
                  "https://github.com/sormuras/hello/releases/download/1-ea+1/com.github.sormuras.hello@1-ea+1.jar#SIZE=1803"));

      bach.log("CAPTION:Restore external tool programs");
      restorer.restore(
          restorer
              .describeExternalToolProgram("sormuras-hello@07819f3ee7")
              .withAsset(
                  "Hello.java",
                  "https://github.com/sormuras/hello/raw/07819f3ee7/Hello.java#SIZE=200")
              .withAsset(
                  "README.md",
                  """
                  string:
                  # Hello.java Program

                  Find source code at <https://github.com/sormuras/hello/blob/07819f3ee7/Hello.java>
                  """),
          restorer
              .describeExternalToolProgram("sormuras-hello@1-ea+1.java")
              .withAsset(
                  "Hello.java",
                  "https://github.com/sormuras/hello/releases/download/1-ea+1/Hello.java#SIZE=203"),
          restorer
              .describeExternalToolProgram("sormuras-hello@1-ea+1.jar")
              .withAsset(
                  "hello-1-ea+1.jar",
                  "https://github.com/sormuras/hello/releases/download/1-ea+1/hello-1-ea+1.jar#SIZE=909"));

      bach.log("CAPTION:Greet current user");
      bach.run("hello");
      bach.run("sormuras-hello@07819f3ee7");
      bach.run("sormuras-hello@1-ea+1.java");
      bach.run("sormuras-hello@1-ea+1.jar");
    }
  }
}
