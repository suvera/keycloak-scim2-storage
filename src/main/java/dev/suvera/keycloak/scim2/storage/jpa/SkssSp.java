package dev.suvera.keycloak.scim2.storage.jpa;

import lombok.Data;

import javax.persistence.*;

/**
 * author: suvera
 * date: 10/14/2020 12:27 PM
 */
@Data
@Entity
@Table(name = "SKSS_SP")
public class SkssSp {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "END_POINT", nullable = false)
    private String endPoint;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "PASSWORD")
    private String password;
}
