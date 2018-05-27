#!/usr/bin/env bash

echo "`basename "$0"` -- testing main variants in dry-run mode..."

FEATURE='9'
LICENSES=(GPL BCL)

while [ "${FEATURE}" != '99' ]
do
  CODE=$(curl -o /dev/null --silent --head --write-out %{http_code} http://jdk.java.net/${FEATURE})
  if [ "${CODE}" -ge '400' ]; then
    break
  fi
  for LICENSE in "${LICENSES[@]}"; do
    echo
    set -x
    ./install-jdk.sh --dry-run --license ${LICENSE} --feature ${FEATURE}
    { set +x; } 2>/dev/null
  done
  FEATURE=$[${FEATURE} +1]
done
