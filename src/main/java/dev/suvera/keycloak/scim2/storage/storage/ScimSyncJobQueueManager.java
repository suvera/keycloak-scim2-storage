package dev.suvera.keycloak.scim2.storage.storage;

import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import dev.suvera.keycloak.scim2.storage.jpa.ScimSyncJobQueue;
import jakarta.persistence.EntityManager;

public class ScimSyncJobQueueManager {
    private static final Logger log = Logger.getLogger(ScimSyncJob.class);
    
    private EntityManager em;

    public ScimSyncJobQueueManager(KeycloakSession session) {
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public ScimSyncJobQueue enqueueJob(ScimSyncJobQueue job) {
        return enqueueJob(job, false);
    }

    public ScimSyncJobQueue enqueueJobAndResetProcessed(ScimSyncJobQueue job) {
        return enqueueJob(job, true);
    }
    public void dequeueJob(ScimSyncJobQueue job) {
        ScimSyncJobQueue existingJob = em.find(ScimSyncJobQueue.class, job.getId());
        if (existingJob == null) {
            log.warnf("Job %s with action %s cannot be removed as it does not exist", job.getId(), job.getAction());
            return;
        }
        log.debugf("Removing job %s with action %s", job.getId(), job.getAction());
        em.remove(existingJob);
    }

    public void increaseRetry(ScimSyncJobQueue job) {
        if (!em.contains(job)) {
            em.merge(job);
        }

        int current = job.getProcessed();
        job.setProcessed(++current);

        log.debugf("Increased retry count for job %s with action %s", job.getId(), job.getAction());
    }

    private ScimSyncJobQueue enqueueJob(ScimSyncJobQueue job, boolean resetProcessed) {
        ScimSyncJobQueue existingJob = em.createNamedQuery("getJobByIdAndAction", ScimSyncJobQueue.class)
                .setParameter("userId", job.getUserId())
                .setParameter("groupId", job.getGroupId())
                .setParameter("action", job.getAction())
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);

        if (existingJob != null) {
            log.debugf("Job %s with action %s already exists", job.getId(), job.getAction());
            if (resetProcessed) {
                existingJob.setProcessed(0);
                em.persist(existingJob);
            }
            return existingJob;
        }
        
        log.debugf("Persisting job %s with action %s", job.getId(), job.getAction());
        em.persist(job);
        return job;
    }
}
