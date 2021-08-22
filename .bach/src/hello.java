import com.github.sormuras.bach.Bach;

class hello {
  public static void main(String... args) {
    try (var bach = new Bach()) {
      bach.logCaption("Grab external tool layers");
      var grabber = bach.grabber();
      grabber.grab(
          grabber
              .newExternalToolLayerDirectory("sormuras-hello@1-ea+1")
              .withAsset(
                  "com.github.sormuras.hello@1-ea+1.jar",
                  "https://github.com/sormuras/hello/releases/download/1-ea+1/com.github.sormuras.hello@1-ea+1.jar#SIZE=1803"));

      bach.logCaption("Grab external tool programs");
      grabber.grab(
          grabber
              .newExternalToolProgramDirectory("sormuras-hello@07819f3ee7")
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
          grabber
              .newExternalToolProgramDirectory("sormuras-hello@1-ea+1.java")
              .withAsset(
                  "Hello.java",
                  "https://github.com/sormuras/hello/releases/download/1-ea+1/Hello.java#SIZE=203"),
          grabber
              .newExternalToolProgramDirectory("sormuras-hello@1-ea+1.jar")
              .withAsset(
                  "hello-1-ea+1.jar",
                  "https://github.com/sormuras/hello/releases/download/1-ea+1/hello-1-ea+1.jar#SIZE=909"));

      bach.logCaption("Greet current user");
      bach.run("hello", call -> call.with("world 1"));
      bach.run("sormuras-hello@07819f3ee7", call -> call.with("world 2"));
      bach.run("sormuras-hello@1-ea+1.java", call -> call.with("world 3"));
      bach.run("sormuras-hello@1-ea+1.jar", call -> call.with("world 4"));
    }
  }
}
