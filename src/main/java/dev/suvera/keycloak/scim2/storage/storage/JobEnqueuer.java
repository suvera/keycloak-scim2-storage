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
import org.keycloak.timer.TimerProvider;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;

public class JobEnqueuer {
    private static final Logger log = Logger.getLogger(JobEnqueuer.class);
    private KeycloakSession session;
    private EntityManager em;
    private TimerProvider timer;

    public JobEnqueuer(KeycloakSession session) {
        this.session = session;
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        timer = session.getProvider(TimerProvider.class);
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

    public void enqueueUserCreateJob(String realmId, String username) {
        session.getContext().setRealm(session.realms().getRealm(realmId));
        RealmModel realmModel = session.realms().getRealm(realmId);
        UserModel userModel = session.userLocalStorage().getUserByUsername(realmModel, username);

        if (userModel != null) {
            ComponentModel componentModel = realmModel.getComponent(userModel.getFederationLink());
            enqueueUserCreateJob(realmModel, componentModel, userModel);
        }
    }

    public void enqueueUserCreateJob(RealmModel realmModel, ComponentModel componentModel, String userId) {
        if (componentModel == null) {
            log.infof("User with id %s is not bound to a federation plugin, will skip creation of the user update/create sync event.", userId);
        } else {
            ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
            entity.setAction(ScimSyncJob.CREATE_USER);
            entity.setComponentId(componentModel.getId());
            entity.setUserId(userId);

            em.persist(entity);

            timer.scheduleTask(session -> {
                run(session, entity, realmModel, componentModel);
                timer.cancelTask(userId);
            }, 500, userId);

            log.infof("User with id %s scheduled to be added on SCIM", userId);
        }
    }

    public void enqueueUserCreateJob(RealmModel realmModel, ComponentModel componentModel, UserModel userModel) {
        if (componentModel == null) {
            log.infof("User with id %s is not bound to a federation plugin, will skip creation of the user update/create sync event.", userModel.getId());
        } else {
            ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
            entity.setAction(ScimSyncJob.CREATE_USER);
            entity.setComponentId(componentModel.getId());
            entity.setUserId(userModel.getId());

            run(entity, realmModel, componentModel, userModel);

            log.infof("User with id %s scheduled to be added on SCIM", userModel.getId());
        }
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
        if (componentModel == null) {
            log.infof("User with id %s is not bound to a federation plugin, will skip creation of the user delete sync.", userId);
        } else {
            ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
            entity.setAction(ScimSyncJob.DELETE_USER);
            entity.setComponentId(componentModel.getId());
            entity.setUserId(userId);
            entity.setExternalId(externalId);

            run(entity, realmModel, componentModel, null);

            log.infof("User with id %s scheduled to be deleted on SCIM", userId);
        }
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

    private void run(KeycloakSession session2, ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel) {
        ScimSyncJob sync = new ScimSyncJob(session2);
        sync.execute(job, realmModel, componentModel, null);
    }
}
