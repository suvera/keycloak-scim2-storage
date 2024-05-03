package dev.suvera.keycloak.scim2.storage.storage;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.user.SynchronizationResult;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;

public class ScimSyncRunner {
    private KeycloakSessionFactory sessionFactory;
    private KeycloakSession session;
    private ComponentModel model;

    public ScimSyncRunner(KeycloakSessionFactory sessionFactory, ComponentModel model) {
        this.sessionFactory = sessionFactory;
        this.model = model;
        session = sessionFactory.create();
    }

    public SynchronizationResult syncAll(String realmId) {
        SynchronizationResult result = new SynchronizationResult();

        callSyncJobs(session, result);

        KeycloakModelUtils.runJobInTransaction(sessionFactory, kcSession -> {
            kcSession.getContext().setRealm(kcSession.realms().getRealm(realmId));

            List<String> ldapComponentModels = ComponentModelUtils
                    .getLDAPComponentsWithScimEventsEnabled(sessionFactory, realmId)
                    .collect(Collectors.toList());

            RealmModel realm = kcSession.realms().getRealm(realmId);
            Stream<UserModel> users = kcSession
                    .users()
                    .searchForUserStream(realm, Map.of(UserModel.ENABLED, "true"))
                    .filter(u -> u.getFederationLink() != null
                            && (u.getFederationLink().equals(model.getId()))
                            || ldapComponentModels.stream().anyMatch(l -> l.equals(u.getFederationLink())));

            users.forEach(user -> {
                ScimSyncJob sync = new ScimSyncJob(kcSession);

                ScimSyncJobQueue job = new ScimSyncJobQueue();

                String action = ScimSyncJob.CREATE_USER;
                if (!user.getFederationLink().equals(model.getId())) {
                    action = ScimSyncJob.CREATE_USER_EXTERNAL;
                }

                job.setAction(action);
                job.setId(KeycloakModelUtils.generateId());
                job.setRealmId(realm.getId());
                job.setComponentId(model.getId());
                job.setUserId(user.getId());

                sync.execute(job, realm, model, user, result);
            });
        });

        return result;
    }

    public SynchronizationResult syncSince(Date lastSync, String realmId) {
        SynchronizationResult result = new SynchronizationResult();

        callSyncJobs(session, result);

        return result;
    }

    private void callSyncJobs(KeycloakSession session, SynchronizationResult result) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        Stream<ScimSyncJobQueue> jobs = em.createNamedQuery("getPendingJobs", ScimSyncJobQueue.class)
                .setMaxResults(1000)
                .getResultStream();

        if (jobs == null) {
            return;
        }

        jobs.forEach(job -> {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, kcSession -> {
                ScimSyncJob sync = new ScimSyncJob(kcSession);
                sync.execute(job, result);
            });
        });
    }
}
