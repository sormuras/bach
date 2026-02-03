package run;

class Start {
  void main(String... args) {
    Project.ofCurrentWorkingDirectory().start(args);
  }
}
