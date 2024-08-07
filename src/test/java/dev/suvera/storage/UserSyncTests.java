package dev.suvera.storage;

import static org.junit.Assert.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.test.FluentTestsHelper;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.suvera.helpers.FluentTestsHelperWithUserUpdate;
import dev.suvera.keycloak.scim2.storage.storage.SkssStorageProviderFactory;
import jakarta.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class UserSyncTests {
    public static String KEYCLOAK_VERSION = "24.0.4";
    static {
        Properties properties = new Properties();
        try (InputStream input = UserSyncTests.class.getClassLoader().getResourceAsStream("test.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test.properties", e);
        }
        String propertyValue = properties.getProperty("keycloak.version");
        if (!propertyValue.equals("${keycloak.version}")) {
            KEYCLOAK_VERSION = propertyValue;
        }
    }
    public static final int KEYCLOAK_PORT = 8080;
    public static final String PLUGIN_NAME = "scim2-provisioning-keycloak-%s-jar-with-dependencies.jar".formatted(KEYCLOAK_VERSION);
    public static final int MOCK_SERVER_PORT = 8081;

    private static FluentTestsHelperWithUserUpdate testsHelper;
    private static ClientAndServer mockServer;
    
    @ClassRule
    public static GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
        .withExposedPorts(KEYCLOAK_PORT)
        .withCommand("start-dev")
        .withEnv("KEYCLOAK_ADMIN", "admin")
        .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
        .withFileSystemBind("./target/" + PLUGIN_NAME, "/opt/keycloak/providers/" + PLUGIN_NAME, BindMode.READ_ONLY);

    @BeforeClass
    public static void beforeTestClass() throws IOException {
        testsHelper = new FluentTestsHelperWithUserUpdate("http://%s:%s".formatted(keycloak.getHost(), keycloak.getMappedPort(KEYCLOAK_PORT)),
                "admin", "admin",
                FluentTestsHelper.DEFAULT_ADMIN_REALM,
                FluentTestsHelper.DEFAULT_ADMIN_CLIENT,
                FluentTestsHelper.DEFAULT_TEST_REALM)
                .init();
    }

    @AfterClass
    public static void afterTestClass() {
        if (testsHelper != null) {
            testsHelper.close();
        }
    }

    @Before
    public void beforeTest() throws IOException {
        testsHelper.importTestRealm("/test-realm.json");
        mockServer = initMockServer(false);
    }

    @After
    public void afterTest() {
        testsHelper.deleteTestRealm();
        
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    public void createNewUser() throws JsonProcessingException, ClientRegistrationException, InterruptedException {
        addProvider();
        String username = "federated-user";
        testsHelper.createTestUser(username, "test-password");

        verifyUserCreate(mockServer, username, VerificationTimes.once());
    }

    @Test
    public void updateUser() throws ClientRegistrationException, InterruptedException, IOException {
        mockServer.stop();
        ClientAndServer mockServerUserExists = initMockServer(true);

        addProvider();
        String username = "federated-user";
        String email = "federated@email.com";
        testsHelper.createTestUser(username, "passa");

        testsHelper.updateUserEmail(username, email);

        verifyUserEmailPatch(mockServerUserExists, "id-" + username, email, VerificationTimes.once());
        mockServerUserExists.stop();
    }

    @Test
    public void patchUser() throws ClientRegistrationException, InterruptedException, IOException {
        mockServer.stop();
        ClientAndServer mockServerUserExists = initMockServer(true);

        addProvider();
        String username = "federated-user";
        testsHelper.createTestUser(username, "passa");

        verifyUserPatch(mockServerUserExists, "id-" + username, VerificationTimes.once());
        mockServerUserExists.stop();
    }

    @Test
    public void deleteUser() throws JsonProcessingException, ClientRegistrationException, InterruptedException {
        addProvider();
        String username = "federated-user";
        testsHelper.createTestUser(username, "pass");

        // wait for the user to sync
        Thread.sleep(2000);

        testsHelper.deleteTestUser(username);

        verifyUserDelete(mockServer, "id-" + username, VerificationTimes.once());
    }

    @Test
    public void syncChangedUsers() throws ClientRegistrationException, IOException, InterruptedException {
        String notFederated, created, patched;
        notFederated = "not-federated-user";
        created = "federated-user-1";
        patched = "federated-user";

        testsHelper.createTestUser(notFederated, "pass");
        
        String providerId = addProvider();
        testsHelper.createTestUser(created, "pass");

        verifyUserCreate(mockServer, created, VerificationTimes.once());
        
        // simulate server unavailability - creates changed user sync job
        mockServer.stop();
        
        testsHelper.createTestUser(patched, "pass");

        // wait for the user to try sync
        Thread.sleep(4000);
        
        ClientAndServer mockServerUserExists = initMockServer(true);
        testsHelper.getTestRealmResource().userStorage().syncUsers(providerId, "triggerChangedUsersSync");

        verifyUserCreate(mockServerUserExists, created, VerificationTimes.never());
        verifyUserPatch(mockServerUserExists, "id-" + patched, VerificationTimes.atLeast(1));
        mockServerUserExists.stop();
    }

    @Test
    public void syncAllUsers() throws ClientRegistrationException, InterruptedException, IOException {
        String notFederated, created, created2;
        notFederated = "not-federated-user";
        created = "federated-user-1";
        created2 = "federated-user-2";

        testsHelper.createTestUser(notFederated, "pass");
        
        String providerId = addProvider();
        testsHelper.createTestUser(created, "pass");

        verifyUserCreate(mockServer, created, VerificationTimes.once());
        
        // simulate server unavailability
        mockServer.stop();
        
        testsHelper.createTestUser(created2, "pass");

        // wait for the user to try sync
        Thread.sleep(4000);

        ClientAndServer mockServerUserExists = initMockServer(false);
        testsHelper.getTestRealmResource().userStorage().syncUsers(providerId, "triggerFullSync");
        
        verifyUserCreate(mockServerUserExists, created, VerificationTimes.once());
        verifyUserCreate(mockServerUserExists, created2, VerificationTimes.atLeast(1));
        mockServerUserExists.stop();
    }

    private String addProvider() throws JsonProcessingException, ClientRegistrationException {
        String providerId, username, password, clientId;
        providerId = SkssStorageProviderFactory.PROVIDER_ID;
        username = "scim-user";
        password = "scim-password";
        clientId = "scim-client";

        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setProviderId(providerId);
        provider.setProviderType("org.keycloak.storage.UserStorageProvider");
        provider.setName(providerId);

        testsHelper.createDirectGrantClient(clientId);
        testsHelper.createTestUser(username, password);
        
        provider.setConfig(new MultivaluedHashMap<String, String>() {{
            putSingle("endPoint", "http://host.docker.internal:" + MOCK_SERVER_PORT);
            putSingle("authorityUrl",  "http://localhost:%d/realms/test".formatted(KEYCLOAK_PORT));
            putSingle("username", username);
            putSingle("password", password);
            putSingle("clientId", clientId);
        }});
        
        Response response = testsHelper.getTestRealmResource().components().add(provider);
        assertEquals(201, response.getStatus());

        return testsHelper.getCreatedId(response);
    }

    private ClientAndServer initMockServer(boolean returnsListResponseWithUser) throws IOException {
        ClientAndServer mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);

        mockServer
            .when(
                request()
                .withMethod("GET")
                .withPath("/ServiceProviderConfig"))
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(loadStringResource("/service-provider-config.json"), MediaType.APPLICATION_JSON)
            );

        mockServer
            .when(
                request()
                .withMethod("GET")
                .withPath("/ResourceTypes"))
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(loadStringResource("/resource-types.json"), MediaType.APPLICATION_JSON)
            );

        mockServer
            .when(
                request()
                .withMethod("GET")
                .withPath("/Schemas"))
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(loadStringResource("/schemas.json"), MediaType.APPLICATION_JSON)
            );

        if (returnsListResponseWithUser) {
            mockServer
                .when(
                    request()
                    .withMethod("GET")
                    .withPath("/Users"))
                .respond(
                    response()
                        .withStatusCode(200)
                        .withBody(loadStringResource("/list-response-with-user.json"), MediaType.APPLICATION_JSON)
                );
        }

        mockServer
            .when(
                request()
                .withMethod("GET")
                .withPath("/Users"))
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(loadStringResource("/empty-list-response.json"), MediaType.APPLICATION_JSON)
            );

        mockServer
            .when(
                request()
                .withMethod("POST")
                .withPath("/Users"))
            .respond(
                httpRequest -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(httpRequest.getBody().toString());
                    ((ObjectNode) jsonNode).put("id", "id-" + jsonNode.get("userName").asText());

                    return response()
                        .withStatusCode(200)
                        .withBody(jsonNode.toString(), MediaType.APPLICATION_JSON);
                }
            );

        mockServer
            .when(
                request()
                .withMethod("PATCH"))
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(loadStringResource("/user-resource.json"), MediaType.APPLICATION_JSON)
            );

        return mockServer;
    }

    private void verifyUserCreate(ClientAndServer mockServer, String username, VerificationTimes times) throws InterruptedException {
        // wait for the request to finish
        Thread.sleep(2000);
        mockServer.verify(
            request()
            .withMethod("POST")
            .withPath("/Users")
            .withBody(
                JsonBody.json(
                    "{ \"userName\": \"%s\" }".formatted(username),
                    MatchType.ONLY_MATCHING_FIELDS
                )
            ),
            times
        );
    }

    private void verifyUserDelete(ClientAndServer mockServer, String userId, VerificationTimes times) throws InterruptedException {
        // wait for the request to finish
        Thread.sleep(2000);
        mockServer.verify(
            request()
            .withMethod("DELETE")
            .withPath("/Users/%s".formatted(userId)),
            times
        );
    }

    private void verifyUserEmailPatch(ClientAndServer mockServer, String userId, String email, VerificationTimes times) throws InterruptedException {
        // wait for the request to finish
        Thread.sleep(4000);
        mockServer.verify(
            request()
            .withMethod("PATCH")
            .withPath("/Users/%s".formatted(userId))
            .withBody(
                JsonBody.json(
                    "{ \"Operations\": [ { \"op\": \"add\", \"path\": \"emails[type eq work].value\", \"value\": \"%s\" } ] }".formatted(email),
                    MatchType.ONLY_MATCHING_FIELDS
                )
            ),
            times
        );
    }

    private void verifyUserPatch(ClientAndServer mockServer, String userId, VerificationTimes times) throws InterruptedException {
        // wait for the request to finish
        Thread.sleep(4000);
        mockServer.verify(
            request()
            .withMethod("PATCH")
            .withPath("/Users/%s".formatted(userId)),
            times
        );
    }

    private String loadStringResource(String path) throws IOException {
        try (InputStream fis = UserSyncTests.class.getResourceAsStream(path)) {
            return new String(fis.readAllBytes());
        }
    }
}
