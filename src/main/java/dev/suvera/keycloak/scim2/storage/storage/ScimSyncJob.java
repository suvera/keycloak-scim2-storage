package dev.suvera.keycloak.scim2.storage.storage;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.ComponentEntity;
import org.keycloak.storage.user.SynchronizationResult;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;
import dev.suvera.scim2.schema.ex.ScimException;

/**
 * author: suvera
 * date: 10/15/2020 7:47 PM
 */
public class ScimSyncJob {
    public static final String CREATE_USER = "userCreate";
    public static final String CREATE_USER_EXTERNAL = "userCreateExternal";
    public static final String DELETE_USER = "userDelete";
    public static final String CREATE_GROUP = "groupCreate";
    public static final String UPDATE_GROUP = "groupUpdate";
    public static final String DELETE_GROUP = "groupDelete";
    public static final String JOIN_GROUP = "groupJoin";
    public static final String LEAVE_GROUP = "groupLeave";

    private static final Logger log = Logger.getLogger(ScimSyncJob.class);
    private KeycloakSession session;
    private EntityManager em;
    private JobEnqueuer enquerer;

    public ScimSyncJob(KeycloakSession session) {
        this.session = session;
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        enquerer = JobEnqueuerFactory.create(session);
    }

    public void execute(ScimSyncJobQueue job) {
        execute(job, null, null, null, null, null);
    }

    public void execute(ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel,
            UserModel userModel) {
        execute(job, realmModel, componentModel, userModel, null, null);
    }

    public void execute(ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel) {
        execute(job, realmModel, componentModel, null, null, null);
    }

    public void execute(ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel,
            UserModel userModel, SynchronizationResult result) {
        execute(job, realmModel, componentModel, userModel, null, result);
    }

    public void execute(ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel,
            UserModel userModel, GroupModel groupModel) {
        execute(job, realmModel, componentModel, userModel, groupModel, null);
    }

    public void execute(ScimSyncJobQueue job, SynchronizationResult result) {
        execute(job, null, null, null, result);
    }

    public void execute(ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel, UserModel userModel,
            GroupModel groupModel, SynchronizationResult result) {
        try {
            executeJob(job, realmModel, componentModel, userModel, groupModel, result);
            deleteJob(job);
        } catch (ScimException e) {
            increaseRetry(job);
            log.error(e.getMessage(), e);
            if (result != null) {
                result.increaseFailed();
            }
        }
    }

    private void executeJob(ScimSyncJobQueue job, RealmModel realmModel, ComponentModel componentModel,
            UserModel userModel, GroupModel groupModel, SynchronizationResult result) throws ScimException {
        if (realmModel == null) {
            session.getContext().setRealm(session.realms().getRealm(job.getRealmId()));
            realmModel = session.realms().getRealm(job.getRealmId());
        }

        if (job.getAction().equals(CREATE_USER)) {
            createUser(realmModel, job, componentModel, userModel, result);
        } else if (job.getAction().equals(CREATE_USER_EXTERNAL)) {
            createUserExternal(realmModel, job, userModel, result);
        } else if (job.getAction().equals(DELETE_USER)) {
            deleteUser(realmModel, job, componentModel);
            if (result != null) {
                result.increaseRemoved();
            }
        } else if (job.getAction().equals(CREATE_GROUP)) {
            createOrUpdateGroup(realmModel, job, componentModel, groupModel);
        } else if (job.getAction().equals(UPDATE_GROUP)) {
            updateGroupName(realmModel, job);
        } else if (job.getAction().equals(DELETE_GROUP)) {
            deleteGroup(realmModel, job);
        } else if (job.getAction().equals(JOIN_GROUP)) {
            joinGroup(realmModel, job, componentModel, userModel, groupModel);
        } else if (job.getAction().equals(LEAVE_GROUP)) {
            leaveGroup(realmModel, job, componentModel, userModel, groupModel);
        }
    }

    private void deleteJob(ScimSyncJobQueue job) {
        em.remove(em.contains(job) ? job : em.merge(job));
    }

    private void increaseRetry(ScimSyncJobQueue job) {
        if (!em.contains(job)) {
            em.merge(job);
        }

        int current = job.getProcessed();
        job.setProcessed(++current);

        log.debugf("Increased retry count for job %s", job.getId());
    }

    private void createUserExternal(RealmModel realmModel, ScimSyncJobQueue job, UserModel userModel, SynchronizationResult result)
            throws ScimException {
        if (userModel == null) {
            userModel = session.userLocalStorage().getUserById(realmModel, job.getUserId());
        }

        if (userModel == null) {
            log.info("could not find user by id: " + job.getUserId());
            return;
        }

        ComponentEntity componentEntity = ComponentModelUtils
                .getComponents(session.getKeycloakSessionFactory(), realmModel.getId(),
                        SkssStorageProviderFactory.PROVIDER_ID)
                .findFirst()
                .orElse(null);

        if (componentEntity == null) {
            log.info("Cannot find appropriate component.");
            return;
        }

        createUser(realmModel, realmModel.getComponent(componentEntity.getId()), userModel, result);
    }

    private void createUser(RealmModel realmModel, ScimSyncJobQueue job, ComponentModel componentModel,
            UserModel userModel, SynchronizationResult result) throws ScimException {
        if (userModel == null) {
            userModel = session.userLocalStorage().getUserById(realmModel, job.getUserId());
        }

        if (userModel == null) {
            log.info("could not find user by id: " + job.getUserId());
            return;
        }

        if (userModel.getFederationLink() == null) {
            log.infof("User with username %s does not have a federation link.", userModel.getUsername());
            return;
        }

        if (job.getComponentId() != null && !job.getComponentId().equals(userModel.getFederationLink())) {
            log.infof("User with username %s is not managed by federation plugin with id %s.", userModel.getUsername(),
                    job.getComponentId());
            return;
        }

        if (componentModel == null) {
            componentModel = realmModel.getComponent(userModel.getFederationLink());
        }

        if (!componentModel.getProviderId().equals(SkssStorageProviderFactory.PROVIDER_ID)) {
            log.info("Federated user component is not of the correct type.");
            return;
        }

        createUser(realmModel, componentModel, userModel, result);
    }

    private void createUser(RealmModel realmModel, ComponentModel componentModel, UserModel userModel, SynchronizationResult result)
            throws ScimException {
        ScimClient2 scimClient = ScimClient2Factory.getClient(componentModel);
        
        ScimUserAdapter scimUserAdapter = new ScimUserAdapter(session, realmModel, componentModel, userModel);
        scimClient.createOrUpdateUser(scimUserAdapter, result);

        if (result != null) {
            List<GroupModel> userGroups = userModel.getGroupsStream().collect(Collectors.toUnmodifiableList());

            session.groupLocalStorage().getGroupsStream(realmModel).forEach(group -> {
                if (userGroups.stream().anyMatch(userGroup -> userGroup.getId().equals(group.getId()))) {
                    enquerer.enqueueGroupJoinJob(realmModel, componentModel, userModel, group);
                } else {
                    enquerer.enqueueGroupLeaveJob(realmModel, componentModel, userModel, group);
                }
            });
        }
    }

    private void deleteUser(RealmModel realmModel, ScimSyncJobQueue job, ComponentModel componentModel)
            throws ScimException {
        if (componentModel == null) {
            if (job.getComponentId() == null) {
                log.info("Component id is needed to delete user");
                return;
            }

            componentModel = realmModel.getComponent(job.getComponentId());
        }

        if (!componentModel.getProviderId().equals(SkssStorageProviderFactory.PROVIDER_ID)) {
            log.info("Federated user component is not of the correct type.");
            return;
        }

        ScimClient2 scimClient = ScimClient2Factory.getClient(componentModel);
        scimClient.deleteUser(job.getExternalId());
    }

    private void updateGroupName(RealmModel realmModel, ScimSyncJobQueue job) throws ScimException {
        createOrUpdateGroup(realmModel, job, null, null, true);
    }

    private void createOrUpdateGroup(RealmModel realmModel, ScimSyncJobQueue job, ComponentModel componentModel,
            GroupModel groupModel, boolean updateOnly) throws ScimException {
        ScimClient2 scimClient = ScimClient2Factory.getClient(componentModel);

        if (groupModel == null) {
            groupModel = session.groupLocalStorage().getGroupById(realmModel, job.getGroupId());
        }

        if (groupModel == null) {
            log.info("Could not find group by id: " + job.getUserId());
            return;
        }

        ScimGroupAdapter scimGroupAdapter = new ScimGroupAdapter(session, groupModel, realmModel.getId(),
                componentModel.getId());

        if (updateOnly) {
            scimClient.updateGroup(scimGroupAdapter);
        } else {
            scimClient.createOrUpdateGroup(scimGroupAdapter);
        }
    }

    private void createOrUpdateGroup(RealmModel realmModel, ScimSyncJobQueue job, ComponentModel componentModel,
            GroupModel groupModel) throws ScimException {
        if (componentModel == null) {
            for (ComponentModel component : ComponentModelUtils
                    .getComponents(session.getKeycloakSessionFactory(), realmModel, SkssStorageProviderFactory.PROVIDER_ID)
                    .collect(Collectors.toList())) {
                createOrUpdateGroup(realmModel, job, component, groupModel, false);
            }
        } else {
            createOrUpdateGroup(realmModel, job, componentModel, groupModel, false);
        }
    }

    private void deleteGroup(RealmModel realmModel, ScimSyncJobQueue job) throws ScimException {
        for (ComponentModel component : ComponentModelUtils
                .getComponents(session.getKeycloakSessionFactory(), realmModel, SkssStorageProviderFactory.PROVIDER_ID)
                .collect(Collectors.toList())) {
            ScimClient2 scimClient = ScimClient2Factory.getClient(component);

            ScimGroupAdapter scimGroupAdapter = new ScimGroupAdapter(session, job.getGroupId(), realmModel.getId(),
                    component.getId());
            scimClient.deleteGroup(scimGroupAdapter.getExternalId());
            scimGroupAdapter.removeExternalId();
        }
    }

    private void joinGroup(RealmModel realmModel, ScimSyncJobQueue job, ComponentModel componentModel,
            UserModel userModel, GroupModel groupModel) throws ScimException {
        LeaveOrJoinGroupResult result = leaveOrJoinGroup(realmModel, job, componentModel, userModel, groupModel, true);

        if (result.shouldRecreateJob) {
            enquerer.enqueueGroupJoinJob(result.realmModel, result.componentModel, result.userModel, result.groupModel);
        }
    }

    private void leaveGroup(RealmModel realmModel, ScimSyncJobQueue job, ComponentModel componentModel,
            UserModel userModel, GroupModel groupModel) throws ScimException {
        LeaveOrJoinGroupResult result = leaveOrJoinGroup(realmModel, job, componentModel, userModel, groupModel, false);

        if (result.shouldRecreateJob) {
            enquerer.enqueueGroupLeaveJob(result.realmModel, result.componentModel, result.userModel, result.groupModel);
        }
    }

    private LeaveOrJoinGroupResult leaveOrJoinGroup(RealmModel realmModel, ScimSyncJobQueue job, ComponentModel componentModel,
            UserModel userModel, GroupModel groupModel, boolean join) throws ScimException {

        if (userModel == null) {
            userModel = session.userLocalStorage().getUserById(realmModel, job.getUserId());
        }

        if (componentModel == null) {
            componentModel = realmModel.getComponent(userModel.getFederationLink());
        }

        if (groupModel == null) {
            groupModel = session.groupLocalStorage().getGroupById(realmModel, job.getGroupId());
        }

        if (groupModel == null) {
            log.info("Could not find group by id: " + job.getUserId());
            return new LeaveOrJoinGroupResult(false, null, null, null, null);
        }

        ScimGroupAdapter scimGroupAdapter = new ScimGroupAdapter(session, groupModel, realmModel.getId(), componentModel.getId());

        boolean createJobScheduled = false;

        if (scimGroupAdapter.getExternalId() == null) {
            enquerer.enqueueGroupCreateJob(realmModel, componentModel, groupModel);
            createJobScheduled = true;
        }

        ScimUserAdapter scimUserAdapter = new ScimUserAdapter(session, realmModel, componentModel, userModel);

        if (scimUserAdapter.getExternalId() == null) {
            enquerer.enqueueUserCreateJob(realmModel, componentModel, userModel);
            createJobScheduled = true;
        }

        if (createJobScheduled) {
            return new LeaveOrJoinGroupResult(true, realmModel, componentModel, userModel, groupModel);
        }

        ScimClient2 scimClient = ScimClient2Factory.getClient(componentModel);

        if (join) {
            scimClient.joinGroup(scimGroupAdapter, scimUserAdapter);
        }
        else {
            scimClient.leaveGroup(scimGroupAdapter, scimUserAdapter);
        }

        return new LeaveOrJoinGroupResult(false, null, null, null, null);
    }

    private static class LeaveOrJoinGroupResult {

        public LeaveOrJoinGroupResult(
            boolean shouldRecreateJob, RealmModel realmModel, ComponentModel componentModel, 
            UserModel userModel, GroupModel groupModel)
        {
            this.shouldRecreateJob = shouldRecreateJob;
            this.realmModel = realmModel;
            this.componentModel = componentModel;
            this.userModel = userModel;
            this.groupModel = groupModel;
        }

        boolean shouldRecreateJob = false;
        RealmModel realmModel;
        ComponentModel componentModel;
        UserModel userModel;
        GroupModel groupModel;
    }
}
