mvn package exec:java -Pstreams-runner -Dexec.mainClass=com.ibm.streams.beam.sample.temperature.TemperatureSample -Dexec.args="\
--runner=StreamsRunner \
--contextType=STREAMING_ANALYTICS_SERVICE \
--vcapServices=vcap.json \
--serviceName=streaming-analytics \
--beamToolkitDir=target/dependency/com.ibm.streams.beam-1.2.1 \
--jarsToStage=target/example-starter-kit-0.1-SNAPSHOT.jar"
