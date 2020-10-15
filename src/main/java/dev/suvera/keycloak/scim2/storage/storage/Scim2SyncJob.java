package dev.suvera.keycloak.scim2.storage.storage;

import dev.suvera.keycloak.scim2.storage.jpa.SkssJobQueue;
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
import java.util.List;

/**
 * author: suvera
 * date: 10/15/2020 7:47 PM
 */
public class Scim2SyncJob implements Runnable {
    private static final Logger log = Logger.getLogger(SkssStorageProvider.class);
    private final EntityManager em;
    private final KeycloakSession session;

    public Scim2SyncJob(KeycloakSession session) {
        this.session = session;
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public void run() {

        //noinspection InfiniteLoopStatement
        while (true) {

            try {
                performSync();
            } catch (Exception e) {
                log.error(e.getMessage());
            }

            try {
                // Pause for 60 seconds
                //noinspection BusyWait
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }

    private void performSync() {
        List<SkssJobQueue> jobs = getPendingJobs();
        if (jobs == null) {
            return;
        }

        for (SkssJobQueue job : jobs) {
            try {
                executeJob(job);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

            deleteJob(job);
        }
    }

    private RealmModel getRealmModel(String realmId) {
        String sql = "select u from RealmEntity u where u.id = :realmId";

        RealmEntity realm = em.createQuery(sql, RealmEntity.class)
                .setParameter("realmId", realmId)
                .getSingleResult();

        return new RealmAdapter(session, em, realm);
    }

    private void executeJob(SkssJobQueue job) throws Exception {
        ComponentModel component = getComponent(job.getComponentId(), job.getRealmId());
        RealmModel realmModel = getRealmModel(job.getRealmId());

        if (job.getAction().equals("userCreate")) {
            UserEntity userEntity = getUserEntity(job.getUsername(), job.getRealmId());
            if (userEntity == null) {
                if (System.currentTimeMillis() - job.getCreatedOn().getTime() < 3000) {
                    throw new Exception("User " + job.getUsername() + " count not be found");
                }
                return;
            }

            Scim2Client scimClient = new Scim2Client(component);
            scimClient.createUser(new SkssUserModel(
                    session,
                    realmModel,
                    component,
                    new UserAdapter(session, realmModel, em, userEntity)
            ));

        } else if (job.getAction().equals("userDelete")) {
            // TODO: need to work on it
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private UserEntity getUserEntity(String username, String realmId) {
        String sql = "select u from UserEntity u where u.username = :username and u.realmId = :realmId";

        UserEntity userEntity = em.createQuery(sql, UserEntity.class)
                .setParameter("username", username)
                .setParameter("realmId", realmId)
                .getSingleResult();

        return userEntity;
    }

    private List<SkssJobQueue> getPendingJobs() {
        String sql = "select u from SkssJobQueue u where u.processed = 0 order by u.createdOn asc";

        return em.createQuery(sql, SkssJobQueue.class)
                .setMaxResults(100)
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
        String sql = "select u from ComponentEntity u where u.id = :cmpId and u.realmId = :realmId";

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
