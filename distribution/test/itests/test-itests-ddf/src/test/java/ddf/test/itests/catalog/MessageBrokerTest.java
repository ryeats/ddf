/**
 * Copyright 2016 Connexta, LLC
 * <p>
 * Unlimited Government Rights (FAR Subpart 27.4) Government right to use, disclose, reproduce,
 * prepare derivative works, distribute copies to the public, and perform and display publicly, in
 * any manner and for any purpose, and to have or permit others to do so.
 **/
package ddf.test.itests.catalog;

import static org.codice.ddf.itests.common.WaitCondition.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.with;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.ConnectionFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.WaitCondition;
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

    private static final String EXAMPLE_TEST_ROUTE = "sdk.example";

    private static final String SJMS_EXAMPLE_TEST_QUEUE = String.format("sjms:%s",
            EXAMPLE_TEST_ROUTE);

    private static final String SJMS_EXAMPLE_TEST_TOPIC = String.format("sjms:topic:%s",
            EXAMPLE_TEST_ROUTE);

    private static final String MOCK_EXAMPLE_TEST_ROUTE = String.format("mock:%s",
            EXAMPLE_TEST_ROUTE);

    private static final String UNDELIVERED_TEST_QUEUE = "undelivered.test";

    private static final String SJMS_UNDELIVERED_TEST_QUEUE = String.format(
            "sjms:%s?transacted=true",
            UNDELIVERED_TEST_QUEUE);

    private static final String MOCK_UNDELIVERED_TEST_ENDPOINT = String.format("mock:%s.end",
            UNDELIVERED_TEST_QUEUE);

    private static final String DLQ_ADDRESS = "jms.queue.DLQ";

    private static final String UNDELIVERED_MESSAGES_MBEAN_URL =
            "/admin/jolokia/exec/org.codice.ddf.broker.ui.UndeliveredMessages:service=UndeliveredMessages/";

    private static final DynamicUrl GET_UNDELIVERED_MESSAGES_PATH =
            new DynamicUrl(DynamicUrl.SECURE_ROOT,
                    HTTPS_PORT,
                    UNDELIVERED_MESSAGES_MBEAN_URL + "getMessages/" + DLQ_ADDRESS + "/Core/");

    private static final DynamicUrl DELETE_UNDELIVERED_MESSAGES_PATH =
            new DynamicUrl(DynamicUrl.SECURE_ROOT,
                    HTTPS_PORT,
                    UNDELIVERED_MESSAGES_MBEAN_URL + "deleteMessages/" + DLQ_ADDRESS + "/Core/");

    private static final DynamicUrl RESEND_UNDELIVERED_MESSAGES_PATH =
            new DynamicUrl(DynamicUrl.SECURE_ROOT,
                    HTTPS_PORT,
                    UNDELIVERED_MESSAGES_MBEAN_URL + "resendMessages/" + DLQ_ADDRESS + "/Core/");

    private static final String ADMIN_USERNAME = "admin";

    private static final String ADMIN_PASSWORD = "admin";

    private static CamelContext camelContext;

    private static final AtomicBoolean blowUp = new AtomicBoolean(true);

    private String messageId;

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
    }

    @Test
    public void testDynamicRouting() throws Exception {
        MockEndpoint endpoint = camelContext.getEndpoint(MOCK_EXAMPLE_TEST_ROUTE,
                MockEndpoint.class);
        endpoint.expectedBodiesReceived("test Message");
        sendMessage(SJMS_EXAMPLE_TEST_QUEUE, "test Message");
        endpoint.assertIsSatisfied();
    }

    @Test
    public void testUndeliveredMessagesRetry() throws URISyntaxException, InterruptedException {
        MockEndpoint endpoint = camelContext.getEndpoint(MOCK_UNDELIVERED_TEST_ENDPOINT,
                MockEndpoint.class);
        endpoint.expectedBodiesReceived("retry");

        blowUp.set(true);
        sendMessage(SJMS_UNDELIVERED_TEST_QUEUE, "retry");

        String initialDlqMessageId = verifyDlqIsNotEmpty(TIMEOUT_IN_SECONDS, 10);
        endpoint.assertIsNotSatisfied();

        blowUp.set(false);
        resendMessageFromDlq(initialDlqMessageId);

        verifyDlqIsEmpty(TIMEOUT_IN_SECONDS, 10);
        endpoint.assertIsSatisfied();

    }

    @Test
    public void testUndeliveredMessagesDelete() throws URISyntaxException, InterruptedException {
        MockEndpoint endpoint = camelContext.getEndpoint(MOCK_UNDELIVERED_TEST_ENDPOINT,
                MockEndpoint.class);
        endpoint.expectedBodiesReceived("delete");

        blowUp.set(true);
        sendMessage(SJMS_UNDELIVERED_TEST_QUEUE, "delete");

        String initialDlqMessageId = verifyDlqIsNotEmpty(TIMEOUT_IN_SECONDS, 10);
        endpoint.assertIsNotSatisfied();

        blowUp.set(false);
        deleteMessageFromDlq(initialDlqMessageId);

        verifyDlqIsEmpty(TIMEOUT_IN_SECONDS, 10);
        endpoint.assertIsNotSatisfied();

    }

    private String verifyDlqIsNotEmpty(long timeout, long pollingInterval) {
        WaitCondition waitCondition = expect(
                "The Dead Letter Queue is not empty and contains an undelivered message.").within(
                timeout,
                TimeUnit.SECONDS)
                .checkEvery(pollingInterval, TimeUnit.SECONDS)
                .until(() -> {
                    // Get undelivered messages from the DLQ
                    List<Object> responseMap = getMessagesFromDlq();
                    if (!responseMap.isEmpty()) {
                        messageId = (String) ((Map) responseMap.get(0)).get("messageID");
                        return true;
                    }
                    return false;
                });
        if (waitCondition.lastResult()) {
            return messageId;
        }
        return "";
    }

    private boolean verifyDlqIsEmpty(long timeout, long pollingInterval) {
        WaitCondition waitCondition = expect("The Dead Letter Queue is empty.").within(timeout,
                TimeUnit.SECONDS)
                .checkEvery(pollingInterval, TimeUnit.SECONDS)
                .until(() -> {
                    // Retrieve Jolokia response from the UndeliveredMessages MBean
                    List<Object> responseMap = getMessagesFromDlq();
                    if (responseMap.isEmpty()) {
                        return true;
                    }
                    return false;
                });
        if (waitCondition.lastResult()) {
            return true;
        }
        return false;
    }

    private List<Object> getMessagesFromDlq() {
        String getUndeliveredMessagesResponse =
                performMbeanOperation(GET_UNDELIVERED_MESSAGES_PATH.getUrl());
        // Validate the undelivered message from the Jolokia response
        validateJolokiaGetMessagesResponse(getUndeliveredMessagesResponse);
        return with(getUndeliveredMessagesResponse).get("value");
    }

    private void deleteMessageFromDlq(String messageId) {
        int value = with(performMbeanOperation(
                DELETE_UNDELIVERED_MESSAGES_PATH.getUrl() + "[\"" + messageId
                        + "\"]")).get("value");
        if (value != 1) {
            fail("Failed to delete message in the DLQ");
        }
    }

    private void resendMessageFromDlq(String messageId) {
        int value = with(performMbeanOperation(
                RESEND_UNDELIVERED_MESSAGES_PATH.getUrl() + "[\"" + messageId
                        + "\"]")).get("value");
        if (value != 1) {
            fail("Failed to resend message in the DLQ");
        }
    }

    private String performMbeanOperation(String operationUrl) {
        // Retrieve Jolokia response from the UndeliveredMessages MBean
        return given().auth()
                .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
                .when()
                .get(operationUrl)
                .asString();
    }

    private void validateJolokiaGetMessagesResponse(String jolokiaGetMessagesResponse) {
        assertThat(with(jolokiaGetMessagesResponse).get("status"), is(200));
        if (!((List) with(jolokiaGetMessagesResponse).
                get("value")).isEmpty()) {
            assertThat(extractMessage(jolokiaGetMessagesResponse, 0).get("address"),
                    is(DLQ_ADDRESS));

        }
    }

    private Map extractMessage(String jolokiaGetMessagesResponse, int index) {
        return (Map) ((List) with(jolokiaGetMessagesResponse).
                get("value")).get(index);
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
                from(SJMS_EXAMPLE_TEST_TOPIC).to(MOCK_EXAMPLE_TEST_ROUTE);
                from(SJMS_UNDELIVERED_TEST_QUEUE).process(exchange -> {
                    if (blowUp.get()) {
                        throw new Exception("Boom!!!");

                    }
                })
                        .to(MOCK_UNDELIVERED_TEST_ENDPOINT);
            }
        });

        camelContext.start();
    }
}