package dev.suvera.keycloak.scim2.storage.jpa;

import lombok.Data;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import java.util.Date;

/**
 * author: suvera
 * date: 10/15/2020 8:08 PM
 */
@NamedQueries({
    @NamedQuery(name="getPendingJobs", query="select u from ScimSyncJobQueue u where u.processed between 0 and 2 order by u.createdOn asc")
})
@Data
@ToString
@Entity
@Table(name = "SCIM_SYNC_JOB_QUEUE")
public class ScimSyncJobQueue {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "GROUP_ID")
    private String groupId;

    @Column(name = "ACTION", nullable = false)
    private String action;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "COMPONENT_ID")
    private String componentId;

    @Column(name = "PROCESSED")
    private int processed = 0;

    @Column(name = "CREATED_ON")
    private Date createdOn = new Date();

    @Column(name = "EXTERNAL_ID")
    private String externalId;
}
