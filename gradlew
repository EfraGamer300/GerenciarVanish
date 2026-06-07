#!/bin/sh

##############################################################################
#  Gradle start up script for POSIX
##############################################################################

APP_HOME=$( cd "${0%"${0##*/}"}" > /dev/null && pwd -P )

MAX_FD=maximum

warn () { echo "$*" >&2; }
die () { echo "$*" >&2; exit 1; }

cygwin=false; msys=false; darwin=false; nonstop=false
case "$( uname )" in
  CYGWIN* )         cygwin=true  ;;
  Darwin* )         darwin=true  ;;
  MSYS* | MINGW* )  msys=true   ;;
  NonStop* )        nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    [ -x "$JAVACMD" ] || die "ERROR: JAVA_HOME invalid: $JAVA_HOME"
else
    JAVACMD=java
    command -v java > /dev/null 2>&1 || die "ERROR: java not found. Install Java 21."
fi

DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
