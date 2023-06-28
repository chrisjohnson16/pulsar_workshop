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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;

public class IoTSensorKafkaConsumer extends S4KCmdApp {
    // Must be set before initializing the "logger" object.
    private final static String APP_NAME = "IoTSensorKafkaConsumer";
    static { System.setProperty("log_file_base_name", getLogFileName(API_TYPE, APP_NAME)); }
    private final static Logger logger = LoggerFactory.getLogger(IoTSensorKafkaConsumer.class);
    private static Consumer<String, String> kafkaConsumer;
    private String consumerGroupId;

    public IoTSensorKafkaConsumer(String appName, String[] inputParams) {
        super(appName, inputParams);
        addRequiredCommandLineOption("cg", "group.id",
                true, "Consumer group ID.");

        logger.info("Starting application: \"" + appName + "\" ...");
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new IoTSensorKafkaConsumer(APP_NAME, args);
        int exitCode = workshopApp.runCmdApp();
        System.exit(exitCode);
    }

    @Override
    public void processExtendedInputParams() throws InvalidParamException {
        super.processExtendedInputParams();

        // (Required) Subscription type identifier
        consumerGroupId = processStringInputParam("cg");
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        Properties properties = getBaseKafkaCfgProperties();
        properties.put("group.id", consumerGroupId);
        properties.put("enable.auto.commit", "false");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(properties);
    }

    @Override
    public void execute() throws WorkshopRuntimException {
        if (kafkaConsumer == null) {
            kafkaConsumer = createKafkaConsumer();
        }

        kafkaConsumer.subscribe(Collections.singletonList(topicName));

        int msgRecvd = 0;
        if (numMsg == -1) {
            numMsg = Integer.MAX_VALUE;
        }

        while (msgRecvd < numMsg) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(1000);
            kafkaConsumer.commitSync();
            for (ConsumerRecord<String, String> record : records) {
                logger.info("({}) Message received and acknowledged: " +
                                "key={}; headers={}; value={}",
                        consumerGroupId,
                        record.key(),
                        record.headers(),
                        record.value());
                msgRecvd++;
            }
        }
    }

    @Override
    public void termCmdApp() {
        try {
            if (kafkaConsumer != null) {
                kafkaConsumer.close();
            }
        }
        finally {
            logger.info("Terminating application: \"" + appName + "\" ...");
        }
    }
}
