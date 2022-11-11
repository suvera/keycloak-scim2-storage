package dev.suvera.scim2.schema.data;

import dev.suvera.scim2.schema.data.meta.MetaRecord;
import lombok.Data;

import java.util.Set;

/**
 * author: suvera
 * date: 10/16/2020 11:10 PM
 */
@Data
public abstract class BaseRecord {
    protected String id;
    protected String externalId;
    protected MetaRecord meta;
    protected Set<String> schemas;
}
