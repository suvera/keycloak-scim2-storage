package dev.suvera.scim2.schema.builder;

import com.google.common.collect.ImmutableSet;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.meta.MetaRecord;
import dev.suvera.scim2.schema.data.sp.SpConfig;

import java.util.Collections;

/**
 * author: suvera
 * date: 10/19/2020 1:04 PM
 */
public class DefaultSpConfig {

    public SpConfig serviceProviderConfig() {
        SpConfig sp = new SpConfig();
        sp.setSchemas(ImmutableSet.of(ScimConstant.URN_SP_CONFIG));
        sp.setMeta(new MetaRecord(ScimConstant.NAME_SP_CONFIG));
        sp.setDocumentationUri("");
        sp.setPatch(new SpConfig.Supported(false));
        sp.setBulk(new SpConfig.Bulk(false, 0, 0));
        sp.setFilter(new SpConfig.Filter(false, 0));
        sp.setEtag(new SpConfig.Supported(false));
        sp.setChangePassword(new SpConfig.Supported(false));
        sp.setSort(new SpConfig.Supported(false));
        sp.setAuthenticationSchemes(Collections.emptyList());

        return sp;
    }

}
