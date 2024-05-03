package dev.suvera.keycloak.scim2.storage.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

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
