/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.sjms2.producer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.support.JmsTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class InOutQueueProducerSyncLoadTest extends JmsTestSupport {

    private static final String TEST_DESTINATION_NAME = "in.out.queue.producer.test";

    private MessageConsumer mc1;

    private MessageConsumer mc2;

    public InOutQueueProducerSyncLoadTest() {
    }

    @Override
    protected boolean useJmx() {
        return false;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mc1 = createQueueConsumer(TEST_DESTINATION_NAME + ".request");
        mc2 = createQueueConsumer(TEST_DESTINATION_NAME + ".request");
        mc1.setMessageListener(new MyMessageListener());
        mc2.setMessageListener(new MyMessageListener());
    }

    @Override
    public void tearDown() throws Exception {
        MyMessageListener l1 = (MyMessageListener) mc1.getMessageListener();
        l1.close();
        mc1.close();
        MyMessageListener l2 = (MyMessageListener) mc2.getMessageListener();
        l2.close();
        mc2.close();
        super.tearDown();
    }

    /**
     * Test to verify that when using the consumer listener for the InOut
     * producer we get the correct message back.
     *
     * @throws Exception
     */
    @Test
    public void testInOutQueueProducer() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 1; i <= 5000; i++) {
            int tempI = i;
            Runnable worker = new Runnable() {

                @Override
                public void run() {
                    try {
                        String requestText = "Message " + tempI;
                        String responseText = "Response Message " + tempI;
                        String response = template.requestBody("direct:start",
                                requestText,
                                String.class);
                        Assert.assertNotNull(response);
                        Assert.assertEquals(responseText, response);
                    } catch (Exception e) {
                        log.error("TODO Auto-generated catch block", e);
                    }
                }
            };
            executor.execute(worker);
        }
        while (context.getInflightRepository()
                .size() > 0) {

        }
        executor.shutdown();
        while (!executor.isTerminated()) {
            //
        }
    }

    /*
     * @see org.apache.camel.test.junit4.CamelTestSupport#createRouteBuilder()
     * 
     * @return
     * 
     * @throws Exception
     */
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("log:" + TEST_DESTINATION_NAME + ".in.log?showBody=true")
                        .inOut("sjms:queue:" + TEST_DESTINATION_NAME + ".request" + "?namedReplyTo="
                                + TEST_DESTINATION_NAME
                                + ".response&consumerCount=20&producerCount=40&synchronous=true")
                        .to("log:" + TEST_DESTINATION_NAME + ".out.log?showBody=true");
            }
        };
    }

    protected class MyMessageListener implements MessageListener {
        private MessageProducer mp;

        @Override
        public void onMessage(Message message) {
            try {
                TextMessage request = (TextMessage) message;
                String text = request.getText();

                TextMessage response = getSession().createTextMessage();
                response.setText("Response " + text);
                response.setJMSCorrelationID(request.getJMSCorrelationID());
                if (mp == null) {
                    mp = getSession().createProducer(message.getJMSReplyTo());
                }
                mp.send(response);
            } catch (JMSException e) {
                Assert.fail(e.getLocalizedMessage());
            }
        }

        public void close() throws JMSException {
            mp.close();
        }
    }
}
