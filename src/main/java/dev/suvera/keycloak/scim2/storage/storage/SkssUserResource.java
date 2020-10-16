package dev.suvera.keycloak.scim2.storage.storage;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.unboundid.scim2.common.annotations.Schema;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.types.UserResource;

/**
 * author: suvera
 * date: 10/16/2020 12:00 PM
 */
@Schema(
        id = "urn:ietf:params:scim:schemas:core:2.0:User",
        name = "User",
        description = "User Account"
)
public class SkssUserResource extends UserResource {

    @Override
    @JsonAnySetter
    protected void setAny(String key, JsonNode value) throws ScimException {
        try {
            super.setAny(key, value);
        } catch (ScimException e) {
            // ignore errors
        }
    }
}
