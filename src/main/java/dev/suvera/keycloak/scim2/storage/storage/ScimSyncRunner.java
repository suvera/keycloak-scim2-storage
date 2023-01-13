package dev.suvera.keycloak.scim2.storage.storage;

import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;
import dev.suvera.scim2.schema.ex.ScimException;

public class ScimSyncRunner {
    private static final String TASK_NAME = "ScimSync";
    private static final Logger log = Logger.getLogger(ScimSyncJob.class);

    private KeycloakSessionFactory sessionFactory;
    private KeycloakSession session;
    private TimerProvider timer;

    public ScimSyncRunner(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        session = sessionFactory.create();
        timer = session.getProvider(TimerProvider.class);
    }

    public void run() {
        timer.scheduleTask(this::performSync, 30000, TASK_NAME);
    }

    public void stop() {
        session.close();
        timer.cancelTask(TASK_NAME);
    }

    private void performSync(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        Stream<ScimSyncJobQueue> jobs = em.createNamedQuery("getPendingJobs", ScimSyncJobQueue.class)
            .setMaxResults(50)
            .getResultStream();

        if (jobs == null) {
            return;
        }

        jobs.forEach(job -> {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, kcSession -> {
                ScimSyncJob sync = new ScimSyncJob(kcSession);
    
                log.info("JOB: " + job);
    
                try {
                    sync.executeJob(job);
                    sync.deleteJob(job);
                } catch (ScimException e) {
                    sync.increaseRetry(job);
                    log.error(e.getMessage(), e);
                }
            });
        });
    }
}
