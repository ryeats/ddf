/**
 * Copyright 2016 Connexta, LLC
 * <p>
 * Unlimited Government Rights (FAR Subpart 27.4) Government right to use, disclose, reproduce,
 * prepare derivative works, distribute copies to the public, and perform and display publicly, in
 * any manner and for any purpose, and to have or permit others to do so.
 **/
package ddf.test.itests.catalog;

import static org.codice.ddf.itests.common.WaitCondition.expect;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class MessageBrokerTest extends AbstractIntegrationTest {

    private static final int TIMEOUT_IN_SECONDS = 60;

    private static final String[] REQUIRED_APPS =
            {"catalog-app", "solr-app", "spatial-app", "broker-app"};

    private static final List<String> LIST_MESSAGES = new ArrayList<>();

    private static CamelContext camelContext;

    @BeforeExam
    public void beforeExam() throws Exception {
        basePort = getBasePort();
        getAdminConfig().setLogLevels();

        getServiceManager().waitForRequiredApps(REQUIRED_APPS);
        setupCamelContext();
        configureRestForGuest();
        getSecurityPolicy().waitForGuestAuthReady(REST_PATH.getUrl() + "?_wadl");
        FileUtils.copyInputStreamToFile(getFileContentAsStream("sdk-example-route.xml",
                AbstractIntegrationTest.class), Paths.get(ddfHome, "etc", "routes")
                .resolve("sdk-example-route.xml")
                .toFile());
    }

    @Before
    public void before() throws Exception {
        LIST_MESSAGES.clear();
    }

    @Test
    public void testDynamicRouting() throws Exception {
        sendMessage("sjms:jms.queue.sdk.example", "test Message");
        expect("A message should go through the flow and not be empty").within(TIMEOUT_IN_SECONDS,
                TimeUnit.SECONDS)
                .until(() -> !(LIST_MESSAGES.size() == 0) && !StringUtils.isEmpty(LIST_MESSAGES.get(
                        0)));
    }

    private void sendMessage(String topicName, String message) throws URISyntaxException {
        camelContext.createProducerTemplate()
                .sendBody(topicName, message);
    }

    private void setupCamelContext() throws Exception {
        camelContext = new DefaultCamelContext();
        final ConnectionFactory factory = getServiceManager().getService(ConnectionFactory.class);
        SjmsComponent sjms = new SjmsComponent();
        sjms.setConnectionFactory(factory);
        camelContext.addComponent("sjms", sjms);
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("sjms:topic:jms.topic.sdk.example").process(exchange -> LIST_MESSAGES.add((String) exchange.getIn()
                        .getBody()))
                        .stop();
            }
        });

        camelContext.start();
    }
}