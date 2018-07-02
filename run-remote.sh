java -cp ./target/example-starter-kit-0.1-SNAPSHOT.jar:target/dependency/com.ibm.streams.beam-1.2.1/com.ibm.streams.beam/lib/com.ibm.streams.beam.translation.jar \
com.ibm.streams.beam.sample.temperature.TemperatureSample \
--runner=StreamsRunner \
--contextType=STREAMING_ANALYTICS_SERVICE \
--vcapServices=vcap.json \
--serviceName=streaming-analytics \
--beamToolkitDir=target/dependency/com.ibm.streams.beam-1.2.1 \
--jarsToStage=target/example-starter-kit-0.1-SNAPSHOT.jar
