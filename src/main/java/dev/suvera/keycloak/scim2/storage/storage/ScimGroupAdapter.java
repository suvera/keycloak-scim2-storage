package dev.suvera.keycloak.scim2.storage.storage;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

import dev.suvera.keycloak.scim2.storage.jpa.FederatedGroupAttributeEntity;

public class ScimGroupAdapter {
    private static final String GROUP_EXTERNAL_ID_ATTRIBUTE = "EXTERNAL_ID";
    
    private GroupModel groupModel;
    private EntityManager em;
    private String realmId;
    private String storageProviderId;
    private String groupId;

    public ScimGroupAdapter(KeycloakSession session, GroupModel groupModel, String realmId, String storageProviderId) {
        this(session, groupModel.getId(), realmId, storageProviderId);
        this.groupModel = groupModel;
    }

    public ScimGroupAdapter(KeycloakSession session, String groupId, String realmId, String storageProviderId) {
        this.groupId = groupId;
        this.realmId = realmId;
        this.storageProviderId = storageProviderId;
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public void setExternalId(String externalId) {
        FederatedGroupAttributeEntity groupAttributeEntity = getFederateGroupAttributeEntity();

        if (groupAttributeEntity == null) {
            groupAttributeEntity = new FederatedGroupAttributeEntity();
            groupAttributeEntity.setId(KeycloakModelUtils.generateId());
            groupAttributeEntity.setName(GROUP_EXTERNAL_ID_ATTRIBUTE);
            groupAttributeEntity.setGroupId(groupId);
            groupAttributeEntity.setRealmId(realmId);
            groupAttributeEntity.setStorageProviderId(storageProviderId);
        }

        groupAttributeEntity.setValue(externalId);
        em.persist(groupAttributeEntity);
    }

    public String getExternalId() {
        FederatedGroupAttributeEntity entity = getFederateGroupAttributeEntity();

        if (entity != null) {
            return entity.getValue();
        }

        return null;
    }

    private FederatedGroupAttributeEntity getFederateGroupAttributeEntity() {
        try {
            return em.createNamedQuery("getFederatedGroupAttribute", FederatedGroupAttributeEntity.class)
            .setParameter("groupId", groupId)
            .setParameter("realmId", realmId)
            .setParameter("storageProviderId", storageProviderId)
            .setParameter("name", GROUP_EXTERNAL_ID_ATTRIBUTE)
            .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void removeExternalId() {
        FederatedGroupAttributeEntity entity = getFederateGroupAttributeEntity();

        if (entity != null) {
            em.remove(entity);
        }
    }

    public GroupModel getGroupModel() {
        return groupModel;
    }
}
