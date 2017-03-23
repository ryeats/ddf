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
package org.apache.camel.component.sjms2;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ExchangePattern;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class SjmsEndpointNameOverrideTest extends CamelTestSupport {

    private static final String BEAN_NAME = "not-sjms";

    @Override
    protected boolean useJmx() {
        return true;
    }

    @Test
    public void testDefaults() throws Exception {
        Endpoint endpoint = context.getEndpoint(BEAN_NAME + ":test");
        Assert.assertNotNull(endpoint);
        Assert.assertTrue(endpoint instanceof Sjms2Endpoint);
        Sjms2Endpoint sjms = (Sjms2Endpoint) endpoint;
        Assert.assertEquals(sjms.getEndpointUri(), BEAN_NAME + "://test");
        Assert.assertEquals(sjms.createExchange()
                .getPattern(), ExchangePattern.InOnly);
    }

    @Test
    public void testQueueEndpoint() throws Exception {
        Endpoint sjms = context.getEndpoint(BEAN_NAME + ":queue:test");
        Assert.assertNotNull(sjms);
        Assert.assertTrue(sjms instanceof Sjms2Endpoint);
        Assert.assertEquals(sjms.getEndpointUri(), BEAN_NAME + "://queue:test");
    }

    @Test
    public void testTopicEndpoint() throws Exception {
        Endpoint sjms = context.getEndpoint(BEAN_NAME + ":topic:test");
        Assert.assertNotNull(sjms);
        Assert.assertTrue(sjms instanceof Sjms2Endpoint);
        Assert.assertEquals(sjms.getEndpointUri(), BEAN_NAME + "://topic:test");
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                "vm://broker?broker.persistent=false&broker.useJmx=false");
        Sjms2Component component = new Sjms2Component();
        component.setConnectionCount(1);
        component.setConnectionFactory(connectionFactory);
        camelContext.addComponent(BEAN_NAME, component);

        return camelContext;
    }
}