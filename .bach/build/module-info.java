module build {
  requires com.github.sormuras.bach;
  provides com.github.sormuras.bach.Bach with build.CustomBach;
}
