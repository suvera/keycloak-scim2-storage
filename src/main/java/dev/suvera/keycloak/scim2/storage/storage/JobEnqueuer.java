package dev.suvera.keycloak.scim2.storage.storage;

import javax.persistence.EntityManager;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;

public class JobEnqueuer {
    private static final Logger log = Logger.getLogger(JobEnqueuer.class);
    private KeycloakSession session;
    private EntityManager em;

    public JobEnqueuer(KeycloakSession session) {
        this.session = session;
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public void enqueueUserUpdateJob(String realmId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setUserId(userId);

        run(entity);

        log.infof("User with id %s scheduled to be updated on SCIM", userId);
    }

    public void enqueueUserCreateJob(RealmModel realmModel, UserModel userModel) {
        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setUserId(userModel.getId());

        run(entity, realmModel, null, userModel);

        log.infof("User with id %s scheduled to be added on SCIM", userModel.getId());
    }

    public void enqueueExternalUserCreateJob(RealmModel realmModel, UserModel userModel) {
        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.CREATE_USER_EXTERNAL);
        entity.setUserId(userModel.getId());

        run(entity, realmModel, null, userModel);

        log.infof("User with id %s scheduled to be added on SCIM", userModel.getId());
    }

    public void enqueueExternalUserCreateJob(RealmModel realmModel, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.CREATE_USER_EXTERNAL);
        entity.setUserId(userId);

        em.persist(entity);

        log.infof("User with id %s scheduled to be added on SCIM", userId);
    }

    public void enqueueUserCreateJob(RealmModel realmModel, ComponentModel componentModel, UserModel userModel) {
        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setComponentId(componentModel.getId());
        entity.setUserId(userModel.getId());

        run(entity, realmModel, componentModel, userModel);

        log.infof("User with id %s scheduled to be added on SCIM", userModel.getId());
    }

    public void enqueueUserCreateJob(String realmId, String componentId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setComponentId(componentId);
        entity.setUserId(userId);

        run(entity);

        log.infof("User with id %s scheduled to be added on SCIM", userId);
    }

    public void enqueueUserDeleteJob(RealmModel realmModel, ComponentModel componentModel, String userId, String externalId) {
        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.DELETE_USER);
        entity.setComponentId(componentModel.getId());
        entity.setUserId(userId);
        entity.setExternalId(externalId);

        run(entity, realmModel, componentModel, null);

        log.infof("User with id %s scheduled to be deleted on SCIM", userId);
    }

    public void enqueueGroupCreateJob(String realmId, String groupId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_GROUP);
        entity.setGroupId(groupId);

        run(entity);

        log.infof("Group with id %s scheduled to be added on SCIM", groupId);
    }

    public void enqueueGroupCreateJob(RealmModel realmModel, ComponentModel componentModel, GroupModel groupModel) {
        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.CREATE_GROUP);
        entity.setGroupId(groupModel.getId());

        run(entity, realmModel, componentModel, null, groupModel);

        log.infof("Group with id %s scheduled to be added on SCIM", groupModel.getId());
    }

    public void enqueueGroupUpdateJob(String realmId, String groupId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.UPDATE_GROUP);
        entity.setGroupId(groupId);

        run(entity);

        log.infof("Group with id %s scheduled to be added on SCIM", groupId);
    }

    public void enqueueGroupDeleteJob(String realmId, String groupId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.DELETE_GROUP);
        entity.setGroupId(groupId);

        run(entity);

        log.infof("Group with id %s scheduled to be deleted on SCIM", groupId);
    }

    public void enqueueGroupJoinJob(String realmId, String groupId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.JOIN_GROUP);
        entity.setGroupId(groupId);
        entity.setUserId(userId);

        run(entity);

        log.infof("User with id %s scheduled to join group with id %s on SCIM", userId, groupId);
    }

    public void enqueueGroupJoinJob(RealmModel realmModel, ComponentModel componentModel, UserModel userModel, GroupModel groupModel) {
        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.JOIN_GROUP);
        entity.setGroupId(groupModel.getId());
        entity.setUserId(userModel.getId());

        run(entity, realmModel, componentModel, userModel, groupModel);

        log.infof("User with id %s scheduled to join group with id %s on SCIM", userModel.getId(), groupModel.getId());
    }

    public void enqueueGroupLeaveJob(String realmId, String groupId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.LEAVE_GROUP);
        entity.setGroupId(groupId);
        entity.setUserId(userId);

        run(entity);

        log.infof("User with id %s scheduled to leave group with id %s on SCIM", userId, groupId);
    }

    public void enqueueGroupLeaveJob(RealmModel realmModel, ComponentModel componentModel, UserModel userModel, GroupModel groupModel) {
        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.LEAVE_GROUP);
        entity.setGroupId(groupModel.getId());
        entity.setUserId(userModel.getId());

        run(entity, realmModel, componentModel, userModel, groupModel);

        log.infof("User with id %s scheduled to join group with id %s on SCIM", userModel.getId(), groupModel.getId());
    }

    private ScimSyncJobQueue createJobQueue(String realmId) {
        ScimSyncJobQueue entity = new ScimSyncJobQueue();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealmId(realmId);
        entity.setProcessed(0);

        return entity;
    }

    private void run(ScimSyncJobQueue job) {
        ScimSyncJob sync = new ScimSyncJob(session);
        sync.execute(job);
    }

    private void run(ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel, UserModel userModel) {
        ScimSyncJob sync = new ScimSyncJob(session);
        sync.execute(job, realmModel, componentModel, userModel);
    }

    private void run(ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel, UserModel userModel, GroupModel groupModel) {
        ScimSyncJob sync = new ScimSyncJob(session);
        sync.execute(job, realmModel, componentModel, userModel, groupModel);
    }
}
