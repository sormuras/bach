package run.bach.external;

import java.util.StringJoiner;

public record GitHubRepository(String user, String repo, String hash) implements Repository {
  @Override
  public String home() {
    var joiner = new StringJoiner("/", "https://github.com/", "");
    joiner.add(user).add(repo);
    if (!hash.equals(DEFAULT.hash)) joiner.add("tree").add(hash);
    return joiner.toString();
  }

  @Override
  public String source(Info info, String name) {
    var joiner = new StringJoiner("/", "https://github.com/", "");
    joiner.add(user).add(repo).add("raw").add(hash);
    joiner.add(".bach").add(info.folder()).add(name + info.extension());
    return joiner.toString();
  }

  @Override
  public String zip() {
    var joiner = new StringJoiner("/", "https://github.com/", ".zip");
    return joiner.add(user).add(repo).add("archive").add(hash).toString();
  }
}
