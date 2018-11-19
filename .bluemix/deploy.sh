#!/bin/bash

echo "TARGET URL ${CF_TARGET_URL}"

if ! [ -x "$(command -v bx)" ]; then
    curl -fsSL https://clis.ng.bluemix.net/install/linux | sh
fi

# TODO : how to know which env to use ?
bx login --apikey $PIPELINE_API_KEY -a https://api.ng.bluemix.net
bx target --cf

bx resource service-key-delete ${CF_APP_NAME}
SA_KEY=$(bx resource service-key-create ${CF_APP_NAME} Manager --instance-name "${SA_INSTANCE}")
echo "generating vcap.json"
echo "{
    \"streaming-analytics\":[{
    \"name\" : \"streaming-analytics\",
    \"credentials\" : {
        \"apikey\": \"$(echo ${SA_KEY} | awk 'BEGIN{FS="apikey: "} {print $2}' | awk '{ print $1 }')\",
        \"v2_rest_url\": \"$(echo ${SA_KEY} | awk 'BEGIN{FS="v2_rest_url: "} {print $2}' | awk '{ print $1 }')\"
     }
   }]}" > vcap.json

cat vcap.json

java -cp ./example-starter-kit-0.1-SNAPSHOT.jar:./dependency/com.ibm.streams.beam-1.2.1/com.ibm.streams.beam/lib/com.ibm.streams.beam.translation.jar \
      com.ibm.streams.beam.sample.temperature.TemperatureSample \
          --runner=StreamsRunner \
          --contextType=STREAMING_ANALYTICS_SERVICE \
          --vcapServices=./vcap.json \
          --serviceName=streaming-analytics \
          --beamToolkitDir=./dependency/com.ibm.streams.beam-1.2.1 \
          --jarsToStage=./example-starter-kit-0.1-SNAPSHOT.jar \
          --jobName=${CF_APP_NAME}