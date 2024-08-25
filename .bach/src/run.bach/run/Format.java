package run;

import run.demo.GoogleJavaFormat;

class Format {
  public static void main(String... args) {
    var tool = new GoogleJavaFormat("1.23.0").install();
    if (args.length == 0) {
      tool.run(call -> call.add("--replace").addFiles("**.java"));
    } else {
      tool.run(args);
    }
  }
}
