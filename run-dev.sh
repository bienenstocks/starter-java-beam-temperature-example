#!/bin/bash
# Simple shell script to run application in dev mode
mvn exec:java -Dexec.classpathScope="runtime" -Dexec.mainClass=com.ibm.streams.beam.sample.temperature.TemperatureSample -Dexec.args="\
--runner=DirectRunner"