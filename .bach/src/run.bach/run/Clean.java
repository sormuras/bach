package run;

class Clean {
  public static void main(String... args) {
    Project.ofCurrentWorkingDirectory().clean();
  }
}
