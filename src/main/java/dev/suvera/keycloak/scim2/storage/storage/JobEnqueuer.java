package dev.suvera.keycloak.scim2.storage.storage;

import javax.persistence.EntityManager;

import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;

public class JobEnqueuer {
    private static final Logger log = Logger.getLogger(JobEnqueuer.class);
    private EntityManager em;

    public JobEnqueuer(KeycloakSession session) {
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public JobEnqueuer(EntityManager em) {
        this.em = em;
    }

    public void enqueueUserCreateJob(String realmId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setUserId(userId);

        em.persist(entity);

        log.infof("User with id %s scheduled to be added on SCIM", userId);
    }

    public void enqueueUserCreateJob(String realmId, String componentId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setComponentId(componentId);
        entity.setUserId(userId);

        em.persist(entity);

        log.infof("User with id %s scheduled to be added on SCIM", userId);
    }

    public void enqueueUserDeleteJob(String realmId, String componentId, String userId, String externalId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.DELETE_USER);
        entity.setComponentId(componentId);
        entity.setUserId(userId);
        entity.setExternalId(externalId);

        em.persist(entity);

        log.infof("User with id %s scheduled to be deleted on SCIM", userId);
    }

    public void enqueueGroupCreateJob(String realmId, String groupId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_GROUP);
        entity.setGroupId(groupId);

        em.persist(entity);

        log.infof("Group with id %s scheduled to be added on SCIM", groupId);
    }

    public void enqueueGroupUpdateJob(String realmId, String groupId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.UPDATE_GROUP);
        entity.setGroupId(groupId);

        em.persist(entity);

        log.infof("Group with id %s scheduled to be added on SCIM", groupId);
    }

    public void enqueueGroupDeleteJob(String realmId, String groupId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.DELETE_GROUP);
        entity.setGroupId(groupId);

        em.persist(entity);

        log.infof("Group with id %s scheduled to be deleted on SCIM", groupId);
    }

    public void enqueueGroupJoinJob(String realmId, String groupId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.JOIN_GROUP);
        entity.setGroupId(groupId);
        entity.setUserId(userId);

        em.persist(entity);

        log.infof("User with id %s scheduled to join group with id %s on SCIM", userId, groupId);
    }

    public void enqueueGroupLeaveJob(String realmId, String groupId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.LEAVE_GROUP);
        entity.setGroupId(groupId);
        entity.setUserId(userId);

        em.persist(entity);

        log.infof("User with id %s scheduled to leave group with id %s on SCIM", userId, groupId);
    }

    private ScimSyncJobQueue createJobQueue(String realmId) {
        ScimSyncJobQueue entity = new ScimSyncJobQueue();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealmId(realmId);
        entity.setProcessed(0);

        return entity;
    }
}
