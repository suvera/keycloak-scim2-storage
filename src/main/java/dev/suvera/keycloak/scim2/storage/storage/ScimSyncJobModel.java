package dev.suvera.keycloak.scim2.storage.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ScimSyncJobModel {
    @Setter
    private ScimSyncJobQueue job;
    private RealmModel realm;
    @Setter
    private ComponentModel component;
    private UserModel user;
    private GroupModel group;

    public ScimSyncJobModel(ScimSyncJobQueue job) {
        this.job = job;
    }

    public void getMissingKeycloakModelsFromSession(KeycloakSession session) {
        if (job == null) {
            return;
        }

        if (realm == null) {
            session.getContext().setRealm(session.realms().getRealm(job.getRealmId()));
            realm = session.realms().getRealm(job.getRealmId());
        }

        if (user == null && realm != null && job.getUserId() != null) {
            user = session.users().getUserById(realm, job.getUserId());
        }
        
        if (component == null && realm != null && user != null) {
            component = realm.getComponent(user.getFederationLink());
        }

        if (group == null && realm != null && job.getGroupId() != null) {
            group = session.groups().getGroupById(realm, job.getGroupId());
        }
    }
}
