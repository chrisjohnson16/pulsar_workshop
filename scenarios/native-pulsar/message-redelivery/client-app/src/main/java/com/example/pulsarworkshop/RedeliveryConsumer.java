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

import com.example.pulsarworkshop.exception.InvalidParamException;
import com.example.pulsarworkshop.exception.WorkshopRuntimException;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedeliveryConsumer extends NativePulsarCmdApp {

    private final static Logger logger = LoggerFactory.getLogger(RedeliveryConsumer.class);

    private final static String APP_NAME = "RedeliveryConsumer";
    private final String SUB_NAME = "demo-subscription";
    private PulsarClient pulsarClient;
    private Consumer<?> pulsarConsumer;

    private String deadLetterTopicName;

    public RedeliveryConsumer(String appName, String[] inputParams) {
        super(appName, inputParams);

        addRequiredCommandLineOption("dlt", "deadLetterTopic", true, 
        			"Pulsar dead letter topic where message go if redelivery fails.");

        logger.info("Starting application: \"" + appName + "\" ...");                    
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new RedeliveryConsumer(APP_NAME, args);
        int exitCode = workshopApp.runCmdApp();
        System.exit(exitCode);
    }

    @Override
    public void processExtendedInputParams() throws InvalidParamException {
        // (Required) Pulsar dead letter topic
        deadLetterTopicName = processStringInputParam("dlt");
    }

    @Override
    public void execute() {

        try {
            pulsarClient = createNativePulsarClient();
            System.out.println("########### Using dlt: " + deadLetterTopicName);

            Consumer<byte[]> pulsarConsumer = pulsarClient.newConsumer()
                    .ackTimeout(1, TimeUnit.SECONDS)
                    .topic(topicName)
                    .subscriptionName(SUB_NAME)
                    .subscriptionType(SubscriptionType.Shared)
                    .deadLetterPolicy(DeadLetterPolicy.builder()
                            .maxRedeliverCount(5)
                            .deadLetterTopic(deadLetterTopicName)
                            .build())
                    .subscribe();

        	// Negative Acknowledge message until re-delivery attempts are exceeded
        	while (true) {
                Message<byte[]> message = pulsarConsumer.receive();
            	System.out.println("########### Received message: " + new String(message.getData()));
                pulsarConsumer.negativeAcknowledge(message);
        	}
        }
        catch (Exception e) {
        	e.printStackTrace();
        	throw new WorkshopRuntimException("Unexpected error when consuming Pulsar messages: " + e.getMessage());
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
    }
}