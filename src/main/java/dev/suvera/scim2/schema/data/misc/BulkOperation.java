package dev.suvera.scim2.schema.data.misc;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.suvera.scim2.schema.data.BaseRecord;
import dev.suvera.scim2.schema.enums.HttpMethod;
import lombok.Data;

/**
 * author: suvera
 * date: 10/17/2020 12:06 PM
 */
@SuppressWarnings("unused")
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BulkOperation {
    private HttpMethod method;
    private String bulkId;
    private String version;

    private String path;
    private BaseRecord data;

    /**
     * response
     */
    private String location;
    private Object response;
    private Integer status;
}
