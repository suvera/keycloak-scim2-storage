package dev.suvera.keycloak.scim2.storage.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import lombok.Data;
import lombok.ToString;

@NamedQueries({
    @NamedQuery(name="getFederatedGroupAttribute", query="select attr from FederatedGroupAttributeEntity attr where attr.groupId = :groupId and lower(attr.realmId) = lower(:realmId) and attr.storageProviderId = :storageProviderId and name = :name")
})
@Data
@ToString
@Entity
@Table(name = "FED_GROUP_ATTRIBUTE")
public class FederatedGroupAttributeEntity {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "GROUP_ID", nullable = false)
    private String groupId;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    private String storageProviderId;

    @Column(name = "VALUE", nullable = false)
    private String value;
}
