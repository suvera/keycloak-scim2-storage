package dev.suvera.scim2.schema.data.misc;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.suvera.scim2.schema.enums.PatchOp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author: suvera
 * date: 10/17/2020 12:06 PM
 */
@SuppressWarnings("unused")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
@NoArgsConstructor
@AllArgsConstructor
public class PatchOperation {
    private PatchOp op;
    private String path;
    private Object value;
}
