#!/bin/bash

wDir=$(cd $(dirname $0) ; pwd -P)
MMT_HOME=$(cd $wDir/../ ; pwd -P)

options="-Dmmt.processing.models=${MMT_HOME}/build/res/"
jar=${MMT_HOME}/build/mmt-*.jar
module=eu.modernmt.cli.PreprocessorMain
java ${options} -cp ${jar} ${module} $@
