#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JHOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
B=$(mktemp -d); trap 'rm -rf "$B"' EXIT
J=app/src/main/java/dev/pgbr/geolab
g++ -std=c++17 -Wall -Wextra -Werror -shared -fPIC -I"$JHOME/include" -I"$JHOME/include/linux" app/src/main/cpp/pipe.cpp -o "$B/libgeolab_pipe.so"
javac --release 17 -d "$B" $(find "$J" -name '*.java' ! -name MainActivity.java) tests/GeoLabTest.java
java -Djava.library.path="$B" -cp "$B" dev.pgbr.geolab.GeoLabTest
