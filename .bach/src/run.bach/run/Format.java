package run;

import run.demo.GoogleJavaFormat;

class Format {
  void main(String... args) {
    var tool = new GoogleJavaFormat("1.34.0").install();
    if (args.length == 0) {
      tool.run(call -> call.add("--replace").addFiles("**.java"));
    } else {
      tool.run(args);
    }
  }
}
