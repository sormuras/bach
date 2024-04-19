package run;

class Build {
  public static void main(String... args) {
    Project.ofCurrentWorkingDirectory().build();
  }
}
