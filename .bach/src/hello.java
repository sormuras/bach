import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;

class hello {
  public static void main(String... args) throws Exception {
    try (var bach = new Bach()) {
      bach.log("caption:Restore external tool providers");
      bach.run(
          "restore",
          ".bach/external-tool-providers/sormuras-hello@1-ea+1/com.github.sormuras.hello@1-ea+1.jar"
              + "=https://github.com/sormuras/hello/releases/download/1-ea+1/com.github.sormuras.hello@1-ea+1.jar#SIZE=1803");

      bach.log("caption:Restore external tool programs");
      bach.runParallel(
          Call.tool(
              "restore",
              ".bach/external-tool-programs/sormuras-hello@07819f3ee7/Hello.java"
                  + "=https://github.com/sormuras/hello/raw/07819f3ee7/Hello.java#SIZE=200"),
          Call.tool(
              "restore",
              ".bach/external-tool-programs/sormuras-hello@1-ea+1.java/Hello.java"
                  + "=https://github.com/sormuras/hello/releases/download/1-ea+1/Hello.java#SIZE=203"),
          Call.tool(
              "restore",
              ".bach/external-tool-programs/sormuras-hello@1-ea+1.jar/hello-1-ea+1.jar"
                  + "=https://github.com/sormuras/hello/releases/download/1-ea+1/hello-1-ea+1.jar#SIZE=909"));

      bach.log("caption:Greet current user");
      bach.run("hello");
      bach.run("sormuras-hello@07819f3ee7");
      bach.run("sormuras-hello@1-ea+1.java");
      bach.run("sormuras-hello@1-ea+1.jar");
    }
  }
}
