/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.example.pulsarworkshop;

import com.example.pulsarworkshop.pojo.IoTSensorData;
import com.example.pulsarworkshop.exception.InvalidParamException;
import com.example.pulsarworkshop.exception.WorkshopRuntimException;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoTSensorConsumerAvro extends NativePulsarCmdApp {

    // Must be set before initializing the "logger" object.
    private final static String APP_NAME = "IoTSensorConsumerAvro";
    static { System.setProperty("log_file_base_name", getLogFileName(API_TYPE, APP_NAME)); }
    private final static Logger logger = LoggerFactory.getLogger(IoTSensorConsumerAvro.class);

    private String subscriptionName;
    private SubscriptionType subscriptionType = SubscriptionType.Exclusive;

    private static PulsarClient pulsarClient;

    private static Consumer<IoTSensorData> pulsarConsumer;

    public IoTSensorConsumerAvro(String appName, String[] inputParams) {
        super(appName, inputParams);
        addOptionalCommandLineOption("sbt","subType", true, "Pulsar subscription type.");
        addRequiredCommandLineOption("sbn", "subName", true, "Pulsar subscription name.");

        logger.info("Starting application: \"" + appName + "\" ...");
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new IoTSensorConsumerAvro(APP_NAME, args);
        int exitCode = workshopApp.runCmdApp();
        System.exit(exitCode);
    }

    @Override
    public void processExtendedInputParams() throws InvalidParamException {
        super.processExtendedInputParams();

        // (Required) Pulsar subscription name
        subscriptionName = processStringInputParam("sbn");
        if ( StringUtils.isBlank(subscriptionName) ) {
            throw new InvalidParamException("Must provide a subscription name for a consumer!");
        }

        // (Optional) Pulsar subscription type
        String subType = processStringInputParam("sbt");
        if (!StringUtils.isBlank(subType)) {
        try {
	            subscriptionType = SubscriptionType.valueOf(subType);
	        }
	        catch (IllegalArgumentException iae) {
	            subscriptionType = SubscriptionType.Exclusive;
	        }
        }
    }

    @Override
    public void execute() {
        try {
            if (pulsarClient == null) {
                pulsarClient = createNativePulsarClient();

                if (pulsarConsumer == null) {
                    ConsumerBuilder<IoTSensorData> consumerBuilder = pulsarClient.newConsumer(Schema.AVRO(IoTSensorData.class));
                    consumerBuilder.topic(topicName);
                    consumerBuilder.subscriptionName(subscriptionName);
                    consumerBuilder.subscriptionType(subscriptionType);
                    pulsarConsumer = consumerBuilder.subscribe();
                }
            }

            int msgRecvd = 0;
            if (numMsg == -1) {
                numMsg = Integer.MAX_VALUE;
            }

            while (msgRecvd < numMsg) {
                Message<IoTSensorData> message = pulsarConsumer.receive();
                IoTSensorData sensorData = message.getValue();
                logger.info("({}) Message received and acknowledged: " +
                                "key={}; properties={}; value={}",
                        pulsarConsumer.getConsumerName(),
                        message.getKey(),
                        message.getProperties(),
                        sensorData);
                pulsarConsumer.acknowledge(message);
                msgRecvd++;
            }

        }
        catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Unexpected error when consuming Pulsar messages: " + pce.getMessage());
        }
    }

    @Override
    public void termCmdApp() {
        try {
            if (pulsarConsumer != null) {
                pulsarConsumer.close();
            }

            if (pulsarClient != null) {
                pulsarClient.close();
            }
        }
        catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Failed to terminate Pulsar producer or client!");
        }
        finally {
            logger.info("Terminating application: \"" + appName + "\" ...");
        }
    }
}
