package dev.suvera.keycloak.scim2.storage.jpa;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * author: suvera
 * date: 10/15/2020 8:08 PM
 */
@Data
@Entity
@ToString
@Table(name = "SKSS_JOB_QUEUE")
public class SkssJobQueue {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "ACTION", nullable = false)
    private String action;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "COMPONENT_ID", nullable = false)
    private String componentId;

    @Column(name = "PROCESSED")
    private int processed = 0;

    @Column(name = "CREATED_ON")
    private Date createdOn = new Date();

    @Column(name = "EXTERNAL_ID")
    private String externalId;
}
