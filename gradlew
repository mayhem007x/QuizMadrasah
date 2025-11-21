#!/usr/bin/env sh
##############################################################################
# Gradle start up script for UN*X
##############################################################################

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS="-Xmx1536m"

APP_NAME=Gradle
APP_BASE_NAME=$(basename "$0")

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
case "$(uname)" in
  CYGWIN* ) cygwin=true ;;
  MINGW* ) msys=true ;;
esac

# Resolve the location of the script
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done

PRG_DIR=$(dirname "$PRG")

GRADLE_HOME="${PRG_DIR}/.gradle"

exec "${PRG_DIR}/gradle/wrapper/gradle-wrapper.jar" "$@"
