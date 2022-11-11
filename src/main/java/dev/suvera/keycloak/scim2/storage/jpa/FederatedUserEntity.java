package dev.suvera.keycloak.scim2.storage.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
@NamedQueries({
    @NamedQuery(name="getUserByUsername", query="select u from FederatedUserEntity u where u.username = :username"),
    @NamedQuery(name="getUserByEmail", query="select u from FederatedUserEntity u where u.email = :email"),
    @NamedQuery(name="getUserCount", query="select count(u) from FederatedUserEntity u"),
    @NamedQuery(name="getAllUsers", query="select u from FederatedUserEntity u"),
    @NamedQuery(name="searchForUser", query="select u from FederatedUserEntity u where " +
            "( lower(u.username) like :search or u.email like :search ) order by u.username"),
})
@Entity
@Table(name = "FEDERATED_USER_ENTITY")
public class FederatedUserEntity {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "EXTERNAL_ID")
    private String externalId;
}
