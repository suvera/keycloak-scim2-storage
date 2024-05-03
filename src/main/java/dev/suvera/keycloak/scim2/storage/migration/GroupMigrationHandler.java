package dev.suvera.keycloak.scim2.storage.migration;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.suvera.keycloak.scim2.storage.storage.ComponentModelUtils;
import dev.suvera.keycloak.scim2.storage.storage.ScimGroupAdapter;
import dev.suvera.keycloak.scim2.storage.storage.SkssStorageProviderFactory;

public class GroupMigrationHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private KeycloakSession session;

    public GroupMigrationHandler(KeycloakSession session) {
        this.session = session;        
    }

    public void handleGroup(String realmId, String groupJsonString)
            throws JsonMappingException, JsonProcessingException {

        RealmModel realmModel = session.realms().getRealm(realmId);

        JsonNode representationJson = objectMapper.readTree(groupJsonString);

        if (representationJson != null) {

            // find local group
            String groupName = representationJson.get("name").asText();
            GroupModel groupModel = session
                    .groups()
                    .searchForGroupByNameStream(realmModel, groupName, true, null, null)
                    .findFirst()
                    .orElse(null);

            // find migration data if present
            if (groupModel != null) {
                List<String> migrationDataValues = groupModel.getAttributes().get("migration_data");
                if (migrationDataValues != null && migrationDataValues.size() > 0) {
                    JsonNode migrationDataNode = objectMapper.readTree(migrationDataValues.get(0));
                    String externalId = migrationDataNode.get("id").asText();

                    // remove `migration_data` attribute
                    groupModel.removeAttribute("migration_data");

                    // set external id on group
                    for (ComponentModel component : ComponentModelUtils
                            .getComponents(session.getKeycloakSessionFactory(), realmModel,
                                    SkssStorageProviderFactory.PROVIDER_ID)
                            .collect(Collectors.toList())) {

                        ScimGroupAdapter scimGroupAdapter = new ScimGroupAdapter(session, groupModel,
                                realmModel.getId(), component.getId());
                        scimGroupAdapter.setExternalId(externalId);
                    }
                }
            }
        }
    }
}
