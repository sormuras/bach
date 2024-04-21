package run;

class Launch {
  public static void main(String... args) {
    Project.ofCurrentWorkingDirectory().launch(args);
  }
}
