package dev.suvera.keycloak.scim2.storage.storage;

import com.unboundid.scim2.common.types.UserResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * author: suvera
 * date: 10/15/2020 11:22 AM
 */
public class SkssUserModel extends AbstractUserAdapterFederatedStorage {
    private String username;
    private UserModel localModel;
    private UserResource userResource;

    public SkssUserModel(
            KeycloakSession session,
            RealmModel realm,
            ComponentModel componentModel,
            UserModel localModel
    ) {
        super(session, realm, componentModel);
        this.username = localModel.getUsername();
        this.localModel = localModel;

        setUsername(localModel.getUsername());
        setFirstName(localModel.getFirstName());
        setLastName(localModel.getLastName());

        setEmail(localModel.getEmail());
        setEmailVerified(localModel.isEmailVerified());

        setEnabled(localModel.isEnabled());

        Map<String, List<String>> attrs = localModel.getAttributes();
        if (attrs != null) {
            for (String key : attrs.keySet()) {
                setAttribute(key, attrs.get(key));
            }
        }
    }

    public void saveExternalUserId(String componentId, String externalId) {
        setSingleAttribute("skss_id_" + componentId, externalId);
        localModel.setSingleAttribute("skss_id_" + componentId, externalId);
    }

    public void saveExternalUserId(String externalId) {
        saveExternalUserId(storageProviderModel.getId(), externalId);
    }

    public String getExternalUserId(String componentId) {
        return getFirstAttribute("skss_id_" + componentId);
    }

    public String getExternalUserId() {
        return getExternalUserId(storageProviderModel.getId());
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    public UserModel getLocalModel() {
        return localModel;
    }

    public void setLocalModel(UserModel localModel) {
        this.localModel = localModel;
    }

    public UserResource getUserResource() {
        return userResource;
    }

    public void setUserResource(UserResource userResource) {
        this.userResource = userResource;
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Set<RoleModel> set = new HashSet(this.getFederatedRoleMappings());

//        if (this.appendDefaultRolesToRoleMappings()) {
//            set.addAll(DefaultRoles.getDefaultRoles(this.realm));
//        }

        set.addAll(this.getRoleMappingsInternal());
        return set;
    }
}
