package dev.suvera.keycloak.scim2.storage.storage;

import dev.suvera.scim2.schema.ex.ScimException;
import org.keycloak.component.ComponentModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author: suvera
 * date: 10/16/2020 9:31 AM
 */
public class ScimClient2Factory {
    public static final Map<String, ScimClient2> instances = new ConcurrentHashMap<>();

    public static synchronized ScimClient2 getClient(ComponentModel componentModel) throws ScimException {
        if (!instances.containsKey(componentModel.getId())) {
            ScimClient2 scimClient = new ScimClient2(componentModel);

            scimClient.validate();

            instances.put(componentModel.getId(), scimClient);
        }

        return instances.get(componentModel.getId());
    }
}
