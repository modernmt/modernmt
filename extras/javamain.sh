#!/bin/bash

SCRIPT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ -z "$1" ]; then
  >&2 echo "Missing main class name. Usage: $0 MAIN_CLASS_NAME [args]";
  exit 1
fi

CLASS="eu.modernmt.cli.$1"
shift
MMT_HOME=$(cd $SCRIPT_HOME/../ ; pwd -P)
JAR=${MMT_HOME}/build/mmt-*.jar

java -Dmmt.home=$MMT_HOME -Djava.library.path=$MMT_HOME/build/lib -cp $JAR $CLASS $@
