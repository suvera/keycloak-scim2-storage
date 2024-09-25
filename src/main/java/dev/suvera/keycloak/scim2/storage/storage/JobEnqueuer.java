package dev.suvera.keycloak.scim2.storage.storage;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;

public class JobEnqueuer {
    private static final Logger log = Logger.getLogger(JobEnqueuer.class);
    private KeycloakSession session;
    private TimerProvider timer;
    private ScimSyncJobQueueManager queueManager;

    public JobEnqueuer(KeycloakSession session) {
        this.session = session;
        queueManager = ScimSyncJobQueueManagerFactory.create(session);
        timer = session.getProvider(TimerProvider.class);
    }

    public void enqueueUserCreateJob(String realmId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setUserId(userId);

        run(entity);

        log.infof("User with id %s scheduled to be added or updated.", userId);
    }

    public void enqueueExternalUserCreateJob(String realmId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_USER_EXTERNAL);
        entity.setUserId(userId);

        queueManager.enqueueJobAndResetProcessed(entity);

        log.infof("External user with id %s scheduled to be added or updated.", userId);
    }

    public void enqueueUserCreateJobByUsername(String realmId, String username) {
        session.getContext().setRealm(session.realms().getRealm(realmId));
        RealmModel realmModel = session.realms().getRealm(realmId);
        UserModel userModel = session.users().getUserByUsername(realmModel, username);

        if (userModel == null) {
            log.infof("Cannot find user with username %s", username);
            return;
        }

        ScimSyncJobQueue entity = createJobQueue(realmModel.getId());
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setComponentId(userModel.getFederationLink());
        entity.setUserId(userModel.getId());

        run(entity);
        log.infof("User with id %s and username %s scheduled to be added or updated.", userModel.getId(), username);
    }

    public void enqueueUserCreateJob(String realmId, String componentId, String userId) {
        ScimSyncJobQueue entity = createUserJobQueue(realmId, componentId, userId);

        run(entity);

        log.infof("User with id %s scheduled to be added.", userId);
    }

    public void enqueueUserDeleteJob(String realmId, String componentId, String userId, String externalId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.DELETE_USER);
        entity.setComponentId(componentId);
        entity.setUserId(userId);
        entity.setExternalId(externalId);

        run(entity);

        log.infof("User with id %s scheduled to be deleted.", userId);
    }

    public void enqueueGroupCreateJob(String realmId, String groupId) {
        // not supported
    }

    public void enqueueGroupUpdateJob(String realmId, String groupId) {
        // not supported
    }

    public void enqueueGroupDeleteJob(String realmId, String groupId) {
        // not supported
    }

    public void enqueueGroupJoinJob(String realmId, String groupId, String userId) {
        // not supported
    }

    public void enqueueGroupLeaveJob(String realmId, String groupId, String userId) {
        // not supported
    }

    private ScimSyncJobQueue createJobQueue(String realmId) {
        ScimSyncJobQueue entity = new ScimSyncJobQueue();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealmId(realmId);
        entity.setProcessed(0);

        return entity;
    }

    private ScimSyncJobQueue createUserJobQueue(String realmId, String componentId, String userId) {
        ScimSyncJobQueue entity = createJobQueue(realmId);
        entity.setAction(ScimSyncJob.CREATE_USER);
        entity.setComponentId(componentId);
        entity.setUserId(userId);
        
        return entity;
    }

    private void run(ScimSyncJobQueue job) {
        queueManager.enqueueJobAndResetProcessed(job);
        String id = job.getUserId() != null ? job.getUserId() :
                    job.getGroupId() != null ? job.getGroupId() :
                    null;

        if (id == null) {
            throw new IllegalArgumentException("Cannot run the job, neither userId or groupId is available.");
        }
        
        timer.scheduleTask(s -> {
            timer.cancelTask(id);
            ScimSyncJob sync = new ScimSyncJob(s);
            sync.execute(new ScimSyncJobQueue(job));
        }, 500, id);
    }
}
