#! /bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#


CUR_SCRIPT_FOLDER=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SCENARIO_HOMEDIR=$( cd -- "${CUR_SCRIPT_FOLDER}/.." &> /dev/null && pwd )

source "${SCENARIO_HOMEDIR}/../../../_bash/utilities.sh"
# DEBUG=true

echo

##
# Show usage info 
#
usage() {   
   echo
   echo "Usage: runConsumer.sh [-h]" 
   echo "                      -t <topic_name>"
   echo "                      -n <message_number>"
   echo "                      -cc <client_conf_file>"
   echo "                      -g <consumer_group_id>"
   echo "                      [-na]"
   echo "                      [-kp <kafka_properties_file>]"
   echo "                      [srp <schema registryproperties file>]"
   echo "       -h  : Show usage info"
   echo "       -t  : (Required) The topic name to publish messages to."
   echo "       -n  : (Required) The number of messages to consume."
   echo "       -cc : (Required) 'client.conf' file path."
   echo "       -g :  (Required) The consumer group ID."
   echo "       -na : (Optional) Non-Astra Streaming (Astra streaming is the default)."
   echo "       -kp : (Optional) 'kafka.properties' file path (only relevant with non-Astra streaming deployment)."
   echo "       -srp: (Optional) 'schema.registry.properties' file path (only relevant with non-Astra streaming deployment)."
   echo
}

if [[ $# -eq 0 || $# -gt 10 ]]; then
   usage
   errExit 10 "Incorrect input parameter count!"
fi

astraStreaming=1
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h)   usage; exit 0      ;;
      -t)   tpName=$2; shift   ;;
      -n)   msgNum=$2; shift   ;;
      -cc)  clntConfFile=$2; shift ;;
      -g)   groupId=$2; shift   ;;
      -na)  astraStreaming=0;  ;;
      -kp)  kafkaCfgPropFile=$2; shift ;;
      -srp) schemaRegCfgPropFile=$2; shift ;;
      *)  errExit 20 "Unknown input parameter passed: $1" ;;
   esac
   shift
done
debugMsg "astraStreaming=${astraStreaming}"
debugMsg "tpName=${tpName}"
debugMsg "msgNum=${msgNum}"
debugMsg "clntConfFile=${clntConfFile}"
debugMsg "groupId=${groupId}"
debugMsg "topicSubType=${topicSubType}"
debugMsg "kafkaCfgPropFile=${kafkaCfgPropFile}"
debugMsg "schemaRegCfgPropFile=${schemaRegCfgPropFile}"

if [[ -z "${tpName}" ]]; then
   errExit 30 "Must provided a valid topic name in format \"<tenant>/<namespace>/<topic>\"!"
fi

if ! [[ -f "${clntConfFile}" ]]; then
   errExit 40 "The specified 'client.conf' file is invalid!"
fi

if [[ -z "${groupId// }"  ]]; then
   errExit 50 "Must provide a consumer group ID."
fi

clientAppJar="${SCENARIO_HOMEDIR}/target/s4k-pubsub-basic-1.0.0.jar"
if ! [[ -f "${clientAppJar}" ]]; then
  errExit 60 "Can't find the client app jar file. Please first build the programs!"
fi

javaCmd="java -cp ${clientAppJar} \
    com.example.pulsarworkshop.IoTSensorKafkaConsumer \
    -n ${msgNum} -t ${tpName} -c ${clntConfFile} -cg ${groupId}"
if [[ ${astraStreaming} -eq 1 ]]; then
  javaCmd="${javaCmd} -a"
fi
if [[ -n "${kafkaCfgPropFile// }" ]]; then
   javaCmd="${javaCmd} -kp ${kafkaCfgPropFile}"
fi
if [[ -n "${schemaRegCfgPropFile// }" ]]; then
   javaCmd="${javaCmd} -srp ${schemaRegCfgPropFile}"
fi
debugMsg="javaCmd=${javaCmd}"

eval ${javaCmd}
