package dev.suvera.keycloak.scim2.storage.storage;

import dev.suvera.keycloak.scim2.storage.jpa.FederatedUserEntity;
import dev.suvera.keycloak.scim2.storage.jpa.SkssJobQueue;
import dev.suvera.scim2.schema.data.user.UserRecord;
import dev.suvera.scim2.schema.ex.ScimException;

import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.RealmAdapter;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.List;

/**
 * author: suvera
 * date: 10/15/2020 7:47 PM
 */
public class Scim2SyncJob implements Runnable {
    private static final Logger log = Logger.getLogger(Scim2SyncJob.class);
    private final EntityManager em;
    private final KeycloakSession session;

    public Scim2SyncJob(KeycloakSession session) {
        this.session = session;
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public void run() {

        while (true) {

            try {
                performSync();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            try {
                // Pause for 60 seconds
                //noinspection BusyWait
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                break;
            }
        }
    }

    private void performSync() {
        List<SkssJobQueue> jobs = getPendingJobs();
        System.out.println(jobs);
        if (jobs == null) {
            return;
        }

        for (SkssJobQueue job : jobs) {
            log.info("JOB: " + job);
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            try {
                executeJob(job);
            } catch (Exception e) {
                log.error(e.getMessage(), e);

                if (System.currentTimeMillis() - job.getCreatedOn().getTime() < 60000) {
                    tx.rollback();
                    break;
                }
            }

            deleteJob(job);
            tx.commit();
        }
    }

    private RealmModel getRealmModel(String realmId) {
        String sql = "select u from RealmEntity u where u.name = :realmId";

        RealmEntity realm = em.createQuery(sql, RealmEntity.class)
                .setParameter("realmId", realmId)
                .getSingleResult();

        return new RealmAdapter(session, em, realm);
    }

    private void executeJob(SkssJobQueue job) throws Exception {
        RealmModel realmModel = getRealmModel(job.getRealmId());

        ComponentModel component = getComponent(job.getComponentId(), realmModel.getId());
        ScimClient2 scimClient = ScimClient2Factory.getClient(component);

        if (job.getAction().equals("userCreate")) {
            FederatedUserEntity federatedEntity = em.find(FederatedUserEntity.class, job.getUserId());
            if (federatedEntity == null) {
                log.info("could not find user by id: " + job.getUserId());
                return;
            }

            FederatedUserAdapter federatedUserAdapter = new FederatedUserAdapter(session, realmModel, component, federatedEntity);

            UserRecord user = null;
            try {
                user = scimClient.findUserByUsername(federatedUserAdapter.getUsername());
            } catch (ScimException e) {
                user = null;
            }
            
            SkssUserModel skssUser = new SkssUserModel(
                session,
                realmModel,
                component,
                federatedUserAdapter
            );

            if (user == null) {
                scimClient.createUser(skssUser, federatedUserAdapter);
            } else {
                scimClient.updateUser(skssUser);
            }
        } else if (job.getAction().equals("userDelete")) {
            scimClient.deleteUser(job.getExternalId());
        }
    }

    private List<SkssJobQueue> getPendingJobs() {
        String sql = "select u from SkssJobQueue u where u.processed = 0 order by u.createdOn asc";

        return em.createQuery(sql, SkssJobQueue.class)
                .setMaxResults(10)
                .getResultList();
    }

    private void deleteJob(SkssJobQueue job) {
        em.remove(job);
        em.flush();
    }

    private void addAttribute(String name, String value, UserEntity user) {
        UserAttributeEntity attr = new UserAttributeEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setUser(user);
        em.persist(attr);
        em.flush();
    }

    private ComponentModel getComponent(String cmpId, String realmId) {
        String sql = "select u from ComponentEntity u where u.id = :cmpId and u.realm.id = :realmId";

        ComponentEntity c = em.createQuery(sql, ComponentEntity.class)
                .setParameter("realmId", realmId)
                .setParameter("cmpId", cmpId)
                .getSingleResult();

        ComponentModel model = new ComponentModel();
        model.setId(c.getId());
        model.setName(c.getName());
        model.setProviderType(c.getProviderType());
        model.setProviderId(c.getProviderId());
        model.setSubType(c.getSubType());
        model.setParentId(c.getParentId());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (ComponentConfigEntity configEntity : c.getComponentConfigs()) {
            config.add(configEntity.getName(), configEntity.getValue());
        }
        model.setConfig(config);
        return model;
    }
}
