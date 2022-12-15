package run.bach.external;

import java.util.Scanner;

/** A walkable file storage for mapping external assets. */
public interface Repository {
  GitHubRepository DEFAULT = new GitHubRepository("sormuras", "bach-info", "HEAD");

  static Repository of(String slug) {
    if (slug == null || slug.isEmpty()) return DEFAULT;
    var scanner = new Scanner(slug);
    scanner.useDelimiter("/");
    var host = scanner.next(); // "github.com"
    if (host.equals("github.com")) {
      var user = scanner.hasNext() ? scanner.next() : DEFAULT.user();
      var repo = scanner.hasNext() ? scanner.next() : DEFAULT.repo();
      var hash = scanner.hasNext() ? String.join("/", scanner.tokens().toList()) : DEFAULT.hash();
      return new GitHubRepository(user, repo, hash);
    }
    throw new RuntimeException("Repository slug not supported: " + slug);
  }

  String home();

  String source(Info info, String name);

  String zip();
}
