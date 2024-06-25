/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes.configuration;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.cloud.hub.model.dashboard.BrandingConfiguration;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.ModelSanitizerHandler;
import com.optio3.cloud.persistence.SessionHolder;

@Optio3IncludeInApiDefinitions
public class PaneConfiguration
{
    public static class Sanitizer extends ModelSanitizerHandler
    {
        @Override
        public void visit(ModelSanitizerContext context,
                          Target target)
        {
            try
            {
                SessionHolder sessionHolder = context.getService(SessionHolder.class);
                if (SystemPreferenceRecord.getTypedSubValue(sessionHolder, SystemPreferenceTypedValue.PaneConfig, (String) target.value, null) == null)
                {
                    target.remove = true;
                }
            }
            catch (Exception e)
            {
                target.remove = true;
            }
        }

        @Override
        public Class<?> getEntityClassHint()
        {
            return null;
        }
    }

    //--//

    public String id;

    public String name;

    public BrandingConfiguration branding;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setLogoBase64(String logoBase64)
    {
        if (branding == null)
        {
            branding = new BrandingConfiguration();
        }

        branding.logoBase64 = logoBase64;
    }

    public AssetGraphBinding titleInput;

    public AssetGraph graph;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setSharedGraphId(String sharedGraphId)
    {
        HubApplication.reportPatchCall(sharedGraphId);

        // Could look up old shared graph but not as a JSON fixup
    }

    public List<PaneCardConfiguration> elements = Lists.newArrayList();
}
