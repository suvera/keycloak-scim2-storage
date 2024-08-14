package dev.suvera.keycloak.scim2.storage.events;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.suvera.keycloak.scim2.storage.migration.GroupMigrationHandler;
import dev.suvera.keycloak.scim2.storage.storage.JobEnqueuer;

public class ScimEventListener implements EventListenerProvider {
    private static final Logger log = Logger.getLogger(ScimEventListener.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private JobEnqueuer jobQueue;
    private GroupMigrationHandler groupMigrationHandler;

    public ScimEventListener(KeycloakSession session, JobEnqueuer jobQueue) {
        this.jobQueue = jobQueue;
        this.groupMigrationHandler = new GroupMigrationHandler(session);
    }

    @Override
    public void close() {
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.UPDATE_PROFILE) {
            log.infof("Handling event: %s", event.getType());

            jobQueue.enqueueUserCreateJob(event.getRealmId(), event.getUserId());
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        ResourceType resourceType = event.getResourceType();

        if (resourceType == ResourceType.USER) {
            handleUserEvent(event);
        } else if (resourceType == ResourceType.GROUP) {
            handleGroupEvent(event);
        } else if (resourceType == ResourceType.GROUP_MEMBERSHIP) {
            handleGroupMembershipEvent(event);
        } else if (resourceType == ResourceType.REALM) {
            String resourcePath = event.getResourcePath();
            if (resourcePath != null && resourcePath.startsWith("group", 0)) {
                handleRealmGroupEvent(event);
            }
        }
    }

    private void handleUserEvent(AdminEvent event) {
        OperationType operationType = event.getOperationType();

        if (operationType == OperationType.CREATE || operationType == OperationType.UPDATE) {
            logEventHandlingMessage(event);

            JsonNode representationJson = readJsonString(event.getRepresentation());

            String userId = event.getResourcePath().split("/")[1];

            if (representationJson != null) {
                JsonNode usernameNode = representationJson.get("username");

                if (userId != null) {
                    if (operationType == OperationType.CREATE && usernameNode != null) {
                        jobQueue.enqueueUserCreateJobByUsername(event.getRealmId(), usernameNode.asText());
                    } else if (operationType == OperationType.UPDATE) {
                        jobQueue.enqueueUserCreateJob(event.getRealmId(), userId);
                    }
                }
            }
        }
    }

    private void handleGroupEvent(AdminEvent event) {
        OperationType operationType = event.getOperationType();

        logEventHandlingMessage(event);
        JsonNode representationJson = readJsonString(event.getRepresentation());

        if (representationJson != null) {
            JsonNode groupId = representationJson.get("id");

            if (groupId != null) {
                if (operationType == OperationType.CREATE) {
                    jobQueue.enqueueGroupCreateJob(event.getRealmId(), groupId.asText());
                } else if (operationType == OperationType.UPDATE) {
                    jobQueue.enqueueGroupUpdateJob(event.getRealmId(), groupId.asText());
                } else if (operationType == OperationType.DELETE) {
                    handleGroupDeleteEvent(event, groupId.asText());
                }
            }
        }
    }

    private void handleGroupDeleteEvent(AdminEvent event, String groupId) {
        logEventHandlingMessage(event);
        // expected resource path: "groups/118e0637-d562-40ae-a357-e0b8bd71be6d"
        String[] splittedPath = event.getResourcePath().split("/");

        jobQueue.enqueueGroupDeleteJob(event.getRealmId(), splittedPath[splittedPath.length - 1]);
    }

    private void handleGroupMembershipEvent(AdminEvent event) {
        OperationType operationType = event.getOperationType();

        logEventHandlingMessage(event);
        // expected resource path:
        // "users/f420cd38-d492-4ba8-a452-52f662d171a3/groups/118e0637-d562-40ae-a357-e0b8bd71be6d"
        String[] splittedPath = event.getResourcePath().split("/");
        String userId = splittedPath[1];
        String groupId = splittedPath[splittedPath.length - 1];

        if (operationType == OperationType.CREATE) {
            jobQueue.enqueueGroupJoinJob(event.getRealmId(), groupId, userId);
        } else if (operationType == OperationType.DELETE) {
            jobQueue.enqueueGroupLeaveJob(event.getRealmId(), groupId, userId);
        }
    }

    private void handleRealmGroupEvent(AdminEvent event) {
        logEventHandlingMessage(event);
        String groupJsonString = event.getRepresentation();

        try {
            groupMigrationHandler.handleGroup(event.getRealmId(), groupJsonString);
        } catch (Exception e) {
            log.errorf(e, "Error while handling migration of the group [%s]: %s", groupJsonString, e);
        }
    }

    private JsonNode readJsonString(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            log.errorf("Cannot read a JSON string: %s", e.getMessage(), e);
            return null;
        }
    }

    private void logEventHandlingMessage(AdminEvent event) {
        log.debugf("Handling admin event: %s, %s", event.getResourceType(), event.getOperationType());
    }
}
