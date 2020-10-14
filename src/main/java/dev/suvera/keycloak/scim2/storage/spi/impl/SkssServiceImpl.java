package dev.suvera.keycloak.scim2.storage.spi.impl;

import dev.suvera.keycloak.scim2.storage.SkssSpRecord;
import dev.suvera.keycloak.scim2.storage.ex.DuplicateSpException;
import dev.suvera.keycloak.scim2.storage.jpa.SkssSp;
import dev.suvera.keycloak.scim2.storage.spi.SkssService;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.LinkedList;
import java.util.List;

/**
 * author: suvera
 * date: 10/14/2020 1:05 PM
 */
public class SkssServiceImpl implements SkssService {

    private final KeycloakSession session;

    public SkssServiceImpl(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected RealmModel getRealm() {
        return session.getContext().getRealm();
    }


    public List<SkssSpRecord> listSp() {
        List<SkssSp> companyEntities = getEntityManager()
                .createQuery("SELECT c FROM SkssSp c WHERE c.realmId = :realmId", SkssSp.class)
                .setParameter("realmId", getRealm().getId())
                .getResultList();

        List<SkssSpRecord> result = new LinkedList<SkssSpRecord>();
        for (SkssSp entity : companyEntities) {
            result.add(new SkssSpRecord(entity));
        }
        return result;
    }

    public SkssSpRecord findSp(String id) {
        SkssSp entity = getEntityManager().find(SkssSp.class, id);
        return entity == null ? null : new SkssSpRecord(entity);
    }

    public void delete(String id) {
        SkssSp entity = getEntityManager().find(SkssSp.class, id);
        if (entity != null) {
            getEntityManager().remove(entity);
            getEntityManager().flush();
        }
    }

    public SkssSpRecord addSp(SkssSpRecord record) throws DuplicateSpException {
        SkssSp entity = new SkssSp();
        String id;
        boolean isUpdate = false;
        String dupSql = "SELECT c FROM SkssSp c WHERE c.realmId = :realmId and c.endPoint = :endPoint";
        if (record.getId() == null) {
            id = KeycloakModelUtils.generateId();
        } else {
            id = record.getId();
            dupSql += " and c.id != :id ";
            isUpdate = true;
        }

        TypedQuery<SkssSp> query = getEntityManager()
                .createQuery(dupSql, SkssSp.class)
                .setParameter("realmId", getRealm().getId())
                .setParameter("endPoint", record.getEndPoint());
        if (isUpdate) {
            query.setParameter("id", id);
        }

        SkssSp existing = query.getSingleResult();
        if (existing != null) {
            String msg = "There is already existing record with same End Point with name ID " + existing.getId();
            throw new DuplicateSpException(msg);
        }

        entity.setId(id);
        entity.setName(record.getName());
        entity.setRealmId(getRealm().getId());

        entity.setEndPoint(record.getEndPoint());
        entity.setUsername(record.getUsername());
        entity.setPassword(record.getPassword());

        getEntityManager().persist(entity);

        record.setId(id);
        return record;
    }

    public void close() {
        // Nothing to do.
    }
}
