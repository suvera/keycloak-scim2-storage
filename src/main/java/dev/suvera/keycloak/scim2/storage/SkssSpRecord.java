package dev.suvera.keycloak.scim2.storage;

import dev.suvera.keycloak.scim2.storage.jpa.SkssSp;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author: suvera
 * date: 10/14/2020 12:46 PM
 */
@Data
@NoArgsConstructor
public class SkssSpRecord {
    private String id;
    private String name;
    private String realmId;
    private String endPoint;
    private String username;
    private String password;

    public SkssSpRecord(SkssSp entity) {
        id = entity.getId();
        name = entity.getName();
        realmId = entity.getRealmId();
        endPoint = entity.getEndPoint();
        username = entity.getUsername();
        password = entity.getPassword();
    }
}
