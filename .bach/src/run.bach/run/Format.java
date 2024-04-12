package run;

class Format {
  public static void main(String... args) {
    var tool = new GoogleJavaFormat("1.22.0").install();
    if (args.length == 0) {
      tool.run(call -> call.add("--replace").addFiles("**.java"));
    } else {
      tool.run(args);
    }
  }
}
