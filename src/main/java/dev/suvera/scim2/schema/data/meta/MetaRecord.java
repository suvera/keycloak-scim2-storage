package dev.suvera.scim2.schema.data.meta;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * author: suvera
 * date: 10/16/2020 11:11 PM
 */
@Data
@NoArgsConstructor
public class MetaRecord {
    private String resourceType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss'Z'")
    private Date created;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss'Z'")
    private Date lastModified;

    private String location;
    private String version;

    public MetaRecord(String resourceType) {
        this.resourceType = resourceType;
    }

    public MetaRecord(String resourceType, String location) {
        this.resourceType = resourceType;
        this.location = location;
    }

    public MetaRecord(
            String resourceType,
            String location,
            Date created,
            Date lastModified,
            String version
    ) {
        this.resourceType = resourceType;
        this.created = created;
        this.lastModified = lastModified;
        this.location = location;
        this.version = version;
    }
}
