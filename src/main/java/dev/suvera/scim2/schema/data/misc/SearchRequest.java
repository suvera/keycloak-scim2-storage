package dev.suvera.scim2.schema.data.misc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.enums.SortOrder;
import dev.suvera.scim2.schema.util.UrlUtil;
import lombok.Data;

import java.util.Collections;
import java.util.Set;

/**
 * author: suvera
 * date: 10/17/2020 12:06 PM
 */
@SuppressWarnings("unused")
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SearchRequest {
    private Set<String> schemas = Collections.singleton(ScimConstant.URN_SEARCH_REQUEST);
    /**
     * ex:  ["displayName", "userName"]
     */
    private Set<String> attributes;
    private Set<String> excludedAttributes;

    private String filter;

    /**
     * Attribute name
     */
    private String sortBy;

    private SortOrder sortOrder;
    private Integer startIndex;
    private Integer count;

    @JsonIgnore
    public String asQueryString() {
        StringBuilder b = new StringBuilder();

        if (filter != null) {
            b.append("filter=").append(UrlUtil.urlEncode(filter)).append("&");
        }
        if (sortBy != null) {
            b.append("sortBy=").append(UrlUtil.urlEncode(sortBy)).append("&");
        }
        if (sortOrder != null) {
            b.append("filter=").append(sortOrder.name().toLowerCase()).append("&");
        }
        if (startIndex != null) {
            b.append("filter=").append(startIndex).append("&");
        }
        if (count != null) {
            b.append("filter=").append(count).append("&");
        }

        return b.toString();
    }

}
