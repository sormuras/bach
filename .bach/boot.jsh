interface Boot {
  Path CACHE = Path.of(".bach/cache");
  String VERSION = System.getProperty("version", "15-ea+2");
}

if (java.lang.module.ModuleFinder.of(Boot.CACHE).find("com.github.sormuras.bach").isEmpty()) {
  var jar = "com.github.sormuras.bach@" + Boot.VERSION + ".jar";
  var source = "https://github.com/sormuras/bach/releases/download/" + Boot.VERSION + "/" + jar;
  var target = Files.createDirectories(Boot.CACHE).resolve(jar);
  System.out.println("Download " + jar + " to " + Boot.CACHE.toUri());
  try (var stream = new URL(source).openStream()) { Files.copy(stream, target); }
}

/env --module-path .bach/cache --add-modules com.github.sormuras.bach

import com.github.sormuras.bach.*

System.out.print(
  """

  Bach %s
  Java %s
  OS   %s

  """
  .formatted(
      Bach.class.getModule().getDescriptor().version().map(Object::toString).orElse("?"),
      Runtime.version(),
      System.getProperty("os.name")
    )
  )
