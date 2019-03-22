FROM maiwj/curl

LABEL "com.github.actions.name"="Test install-jdk.sh action"
LABEL "com.github.actions.description"="Write arguments to the standard output"
LABEL "com.github.actions.icon"="terminal"
LABEL "com.github.actions.color"="blue"

LABEL "repository"="https://github.com/sormuras/bach/tree/master/.github/action/test-install-jdk-action"
LABEL "homepage"="https://github.com/sormuras/bach"
LABEL "maintainer"="Christian Stein <sormuras@github.com>"

ADD entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
