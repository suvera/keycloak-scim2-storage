package dev.suvera.keycloak.scim2.storage.storage;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.RealmAdapter;
import org.keycloak.models.jpa.entities.ComponentConfigEntity;
import org.keycloak.models.jpa.entities.ComponentEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;
import dev.suvera.scim2.schema.ex.ScimException;

/**
 * author: suvera
 * date: 10/15/2020 7:47 PM
 */
public class ScimSyncJob {
    public static final String CREATE_USER = "userCreate";
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
        enquerer = new JobEnqueuer(em);
    }

    private RealmModel getRealmModel(String realmId) {
        String sql = "select u from RealmEntity u where u.name = :realmId";

        RealmEntity realm = em.createQuery(sql, RealmEntity.class)
                .setParameter("realmId", realmId)
                .getSingleResult();

        return new RealmAdapter(session, em, realm);
    }

    public void executeJob(ScimSyncJobQueue job) throws ScimException {
        RealmModel realmModel = getRealmModel(job.getRealmId());

        if (job.getAction().equals(CREATE_USER)) {
           createUser(realmModel, job);
        }
        else if (job.getAction().equals(DELETE_USER)) {
            deleteUser(realmModel, job);
        }
        else if (job.getAction().equals(CREATE_GROUP)) {
            createOrUpdateGroup(realmModel, job, false);
        }
        else if (job.getAction().equals(UPDATE_GROUP)) {
            createOrUpdateGroup(realmModel, job, true);
        }
        else if (job.getAction().equals(DELETE_GROUP)) {
            deleteGroup(realmModel, job);
        }
        else if (job.getAction().equals(JOIN_GROUP)) {
            joinGroup(realmModel, job);
        }
        else if (job.getAction().equals(LEAVE_GROUP)) {
            leaveGroup(realmModel, job);
        }
    }

    public void deleteJob(ScimSyncJobQueue job) {
        em.remove(em.contains(job) ? job : em.merge(job));
    }

    public void increaseRetry(ScimSyncJobQueue job) {
        if (!em.contains(job)) {
            em.merge(job);
        }

        int current = job.getProcessed();
        job.setProcessed(++current);

        log.debugf("Increased retry count for job %s", job.getId());
    }

    private void createUser(RealmModel realmModel, ScimSyncJobQueue job) throws ScimException {
        UserModel userModel = session.userLocalStorage().getUserById(realmModel, job.getUserId());

        if (userModel == null) {
            log.info("could not find user by id: " + job.getUserId());
            return;
        }

        if (userModel.getFederationLink() == null) {
            log.infof("User with username %s does not have a federation link.", userModel.getUsername());
            return;
        }

        if (job.getComponentId() != null && !job.getComponentId().equals(userModel.getFederationLink())) {
            log.infof("User with username %s is not managed by federation plugin with id %s.", userModel.getUsername(), job.getComponentId());
            return;
        }

        ComponentModel component = getComponent(userModel.getFederationLink(), realmModel.getId());

        if (!component.getProviderId().equals(SkssStorageProviderFactory.PROVIDER_ID)) {
            log.info("Federated user component is not of the correct type.");
            return;
        }
        
        ScimClient2 scimClient = ScimClient2Factory.getClient(component);
        
        ScimUserAdapter scimUserAdapter = new ScimUserAdapter(session, realmModel, component, userModel);
        scimClient.createOrUpdateUser(scimUserAdapter);
    }

    private void deleteUser(RealmModel realmModel, ScimSyncJobQueue job) throws ScimException {
        if (job.getComponentId() == null) {
            log.info("Component id is needed to delete user");
            return;
        }

        ComponentModel component = getComponent(job.getComponentId(), realmModel.getId());

        if (!component.getProviderId().equals(SkssStorageProviderFactory.PROVIDER_ID)) {
            log.info("Federated user component is not of the correct type.");
            return;
        }

        ScimClient2 scimClient = ScimClient2Factory.getClient(component);
        scimClient.deleteUser(job.getExternalId());
    }

    private void createOrUpdateGroup(RealmModel realmModel, ScimSyncJobQueue job, boolean updateOnly) throws ScimException {
        for (ComponentModel component : getComponents(SkssStorageProviderFactory.PROVIDER_ID)) {
            ScimClient2 scimClient = ScimClient2Factory.getClient(component);

            GroupModel groupModel = session.groupLocalStorage().getGroupById(realmModel, job.getGroupId());

            if (groupModel == null) {
                log.info("Could not find group by id: " + job.getUserId());
                return;
            }

            ScimGroupAdapter scimGroupAdapter = new ScimGroupAdapter(session, groupModel, realmModel.getId(), component.getId());
            
            if (updateOnly) {
                scimClient.updateGroup(scimGroupAdapter);
            }
            else {
                scimClient.createOrUpdateGroup(scimGroupAdapter);
            }
        }
    }

    private void deleteGroup(RealmModel realmModel, ScimSyncJobQueue job) throws ScimException {
        for (ComponentModel component : getComponents(SkssStorageProviderFactory.PROVIDER_ID)) {
            ScimClient2 scimClient = ScimClient2Factory.getClient(component);

            ScimGroupAdapter scimGroupAdapter = new ScimGroupAdapter(session, job.getGroupId(), realmModel.getId(), component.getId());
            scimClient.deleteGroup(scimGroupAdapter.getExternalId());
            scimGroupAdapter.removeExternalId();
        }
    }

    private void joinGroup(RealmModel realmModel, ScimSyncJobQueue job) throws ScimException {
        boolean shouldRecreateJob = leaveOrJoinGroup(realmModel, job, true);

        if (shouldRecreateJob) {
            enquerer.enqueueGroupJoinJob(job.getRealmId(), job.getGroupId(), job.getUserId());
        }
    }

    private void leaveGroup(RealmModel realmModel, ScimSyncJobQueue job) throws ScimException {
        boolean shouldRecreateJob = leaveOrJoinGroup(realmModel, job, false);

        if (shouldRecreateJob) {
            enquerer.enqueueGroupLeaveJob(job.getRealmId(), job.getGroupId(), job.getUserId());
        }
    }

    private boolean leaveOrJoinGroup(RealmModel realmModel, ScimSyncJobQueue job, boolean join) throws ScimException {
        UserModel userModel = session.userLocalStorage().getUserById(realmModel, job.getUserId());

        if (userModel.getFederationLink() == null) {
            log.infof("User with username %s does not have a federation link.", userModel.getUsername());
            return false;
        }

        ComponentModel component = getComponent(userModel.getFederationLink(), realmModel.getId());

        if (!component.getProviderId().equals(SkssStorageProviderFactory.PROVIDER_ID)) {
            log.info("Federated user component is not of the correct type.");
            return false;
        }

        GroupModel groupModel = session.groupLocalStorage().getGroupById(realmModel, job.getGroupId());

        if (groupModel == null) {
            log.info("Could not find group by id: " + job.getUserId());
            return false;
        }

        ScimGroupAdapter scimGroupAdapter = new ScimGroupAdapter(session, groupModel, realmModel.getId(), component.getId());

        boolean createJobScheduled = false;

        if (scimGroupAdapter.getExternalId() == null) {
            enquerer.enqueueGroupCreateJob(job.getRealmId(), job.getGroupId());
            createJobScheduled = true;
        }

        ScimUserAdapter scimUserAdapter = new ScimUserAdapter(session, realmModel, component, userModel);

        if (scimUserAdapter.getExternalId() == null) {
            enquerer.enqueueUserCreateJob(job.getRealmId(), userModel.getFederationLink(), job.getUserId());
            createJobScheduled = true;
        }

        ScimClient2 scimClient = ScimClient2Factory.getClient(component);

        boolean result = join ?
            scimClient.joinGroup(scimGroupAdapter, scimUserAdapter) :
            scimClient.leaveGroup(scimGroupAdapter, scimUserAdapter);
        
        return !result && createJobScheduled;
    }

    private List<ComponentModel> getComponents(String providerId) {
        String sql = "select ce from ComponentEntity ce where ce.providerId = :providerId";

        List<ComponentEntity> componentEntityList = em.createQuery(sql, ComponentEntity.class)
                .setParameter("providerId", providerId)
                .getResultList();

        return componentEntityList
                .stream()
                .map(this::mapComponentModel)
                .collect(Collectors.toList());
    }

    private ComponentModel getComponent(String cmpId, String realmId) {
        String sql = "select u from ComponentEntity u where u.id = :cmpId and u.realm.id = :realmId";

        ComponentEntity c = em.createQuery(sql, ComponentEntity.class)
                .setParameter("realmId", realmId)
                .setParameter("cmpId", cmpId)
                .getSingleResult();

        return mapComponentModel(c);
    }

    private ComponentModel mapComponentModel(ComponentEntity componentEntity) {
        ComponentModel model = new ComponentModel();
        model.setId(componentEntity.getId());
        model.setName(componentEntity.getName());
        model.setProviderType(componentEntity.getProviderType());
        model.setProviderId(componentEntity.getProviderId());
        model.setSubType(componentEntity.getSubType());
        model.setParentId(componentEntity.getParentId());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (ComponentConfigEntity configEntity : componentEntity.getComponentConfigs()) {
            config.add(configEntity.getName(), configEntity.getValue());
        }
        model.setConfig(config);
        return model;
    }
}
