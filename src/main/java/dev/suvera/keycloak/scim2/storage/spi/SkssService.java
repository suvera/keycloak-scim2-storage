package dev.suvera.keycloak.scim2.storage.spi;

import dev.suvera.keycloak.scim2.storage.SkssSpRecord;
import dev.suvera.keycloak.scim2.storage.ex.DuplicateSpException;
import org.keycloak.provider.Provider;

import java.util.List;

/**
 * author: suvera
 * date: 10/14/2020 12:48 PM
 */
public interface SkssService extends Provider {
    List<SkssSpRecord> listSp();

    SkssSpRecord findSp(String id);

    SkssSpRecord addSp(SkssSpRecord record) throws DuplicateSpException;

    void delete(String id);
}
