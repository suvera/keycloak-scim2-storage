package dev.suvera.keycloak.scim2.storage.storage;

import com.unboundid.scim2.common.exceptions.ScimException;
import org.keycloak.component.ComponentModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author: suvera
 * date: 10/16/2020 9:31 AM
 */
public class Scim2ClientFactory {
    public static final Map<String, Scim2Client> instances = new ConcurrentHashMap<>();

    public static synchronized Scim2Client getClient(ComponentModel componentModel) throws ScimException {
        if (!instances.containsKey(componentModel.getId())) {
            Scim2Client scimClient = new Scim2Client(componentModel);

            scimClient.validate();

            instances.put(componentModel.getId(), scimClient);
        }

        return instances.get(componentModel.getId());
    }
}
