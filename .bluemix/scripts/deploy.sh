 #!/bin/bash

      wget -O jq https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64
      chmod +x ./jq
      cp jq /usr/bin

      if ! [ -x "$(command -v bx)" ]; then
         curl -fsSL https://clis.ng.bluemix.net/install/linux | sh
      fi

      # TODO : how to know which env to use ?
      bx login --apikey $PIPELINE_API_KEY -a https://api.ng.bluemix.net
      bx target --cf

      # get Streaming analytics credentials
      bx resource service-key-delete "SA_${APP_NAME}" -f
      SA_KEY=$(bx resource service-key-create "SA_${APP_NAME}" Manager --instance-name "${SA_INSTANCE}")


      echo "generating vcap.json"
      echo "{
         \"streaming-analytics\":[{
         \"name\" : \"streaming-analytics\",
         \"credentials\" : {
             \"apikey\": \"$(echo ${SA_KEY} | awk 'BEGIN{FS="apikey: "} {print $2}' | awk '{ print $1 }')\",
             \"v2_rest_url\": \"$(echo ${SA_KEY} | awk 'BEGIN{FS="v2_rest_url: "} {print $2}' | awk '{ print $1 }')\"
          }
        }]" > vcap.json

      if [ $COS_INSTANCE ]; then
        # get COS credentials
        bx resource service-key-delete "COS_${APP_NAME}" -f
        COS_KEY=$(bx resource service-key-create "COS_${APP_NAME}" Manager --instance-name "${COS_INSTANCE}" --p {\"HMAC\":true})
        token=$(curl -X "POST" "https://iam.bluemix.net/oidc/token" \
            -H 'Accept: application/json' \
            -H 'Content-Type: application/x-www-form-urlencoded' \
            --data-urlencode "apikey=$(echo ${COS_KEY} | awk 'BEGIN{FS="apikey: "} {print $2}' | awk '{ print $1 }')" \
            --data-urlencode "response_type=cloud_iam" \
            --data-urlencode "grant_type=urn:ibm:params:oauth:grant-type:apikey" | jq -r '.access_token')
        curl -X "PUT" "https://s3-api.us-geo.objectstorage.softlayer.net/${APP_NAME}" \
            -H "Authorization: Bearer ${token}" \
            -H "ibm-service-instance-id: $(echo ${COS_KEY} | awk 'BEGIN{FS="resource_instance_id: "} {print $2}' | awk '{ print $1 }')"

        echo ",
         \"cos\": {
            \"endpoint\": \"s3-api.us-geo.objectstorage.softlayer.net\",
            \"accessKeyId\": \"$(echo ${COS_KEY} | awk 'BEGIN{FS="access_key_id: "} {print $2}' | awk '{ print $1 }')\",
            \"secretKey\": \"$(echo ${COS_KEY} | awk 'BEGIN{FS="secret_access_key: "} {print $2}' | awk '{ print $1 }')\",
            \"bucket\": \"${APP_NAME}\",
            \"filePrefix\": \"prefix\"
         }" >> vcap.json
      fi

      if [ $MH_INSTANCE ]; then
        # get MH credentials
        bx service key-create "${MH_INSTANCE}" "MH_${APP_NAME}"
        MH_KEY=$(bx service key-create "${MH_INSTANCE}" "MH_${APP_NAME}")
        MH_KEY=$(bx resource service-key-create "MH_${APP_NAME}" Manager --instance-name "${MH_INSTANCE}")
        echo ",
          \"messagehub\": {
            \"user\": \"$(echo ${MH_KEY} | awk 'BEGIN{FS="user: "} {print $2}' | awk '{ print $1 }')\",
            \"password\": \"$(echo ${MH_KEY} | awk 'BEGIN{FS="password: "} {print $2}' | awk '{ print $1 }')\",
            \"kafka_brokers_sasl\": \"$(echo ${MH_KEY} | awk 'BEGIN{FS="kafka_brokers_sasl: "} {print $2}' | awk '{ print $1 }')\"
          }" >>vcap.json
      fi

      echo "}" >> vcap.json
      cat vcap.json

      java -cp ./example-starter-kit-0.1-SNAPSHOT.jar:./dependency/com.ibm.streams.beam-1.2.1/com.ibm.streams.beam/lib/com.ibm.streams.beam.translation.jar \
           com.ibm.streams.beam.sample.temperature.TemperatureSample \
               --runner=StreamsRunner \
               --contextType=STREAMING_ANALYTICS_SERVICE \
               --vcapServices=./vcap.json \
               --serviceName=streaming-analytics \
               --beamToolkitDir=./dependency/com.ibm.streams.beam-1.2.1 \
               --jarsToStage=./example-starter-kit-0.1-SNAPSHOT.jar \
               --jobName=${APP_NAME}