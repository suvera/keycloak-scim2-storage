package dev.suvera.scim2.schema.data.misc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.suvera.scim2.schema.data.BaseRecord;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * author: suvera
 * date: 10/17/2020 12:39 PM
 */
@Data
public class ListResponse<T extends BaseRecord> {

    /**
     * This must be ScimConstant.URN_LIST_RESPONSE
     */
    private Set<String> schemas;

    @NotNull(message = "totalResults cannot be null")
    @PositiveOrZero(message = "totalResults must be 0 or greater")
    private Integer totalResults;
    private Integer itemsPerPage;
    private Integer startIndex;

    @JsonProperty("Resources")
    private List<T> resources;

    @JsonIgnore
    public void addResource(T resource) {
        if (resources == null) {
            resources = new ArrayList<>();
        }
        resources.add(resource);
    }
}
