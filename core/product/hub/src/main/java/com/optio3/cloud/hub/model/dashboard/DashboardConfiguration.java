/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.cloud.hub.model.asset.graph.SharedAssetGraph;
import com.optio3.cloud.hub.model.dashboard.enums.HorizontalAlignment;
import com.optio3.cloud.hub.model.dashboard.enums.VerticalAlignment;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.ModelSanitizerHandler;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@Optio3IncludeInApiDefinitions
public class DashboardConfiguration
{
    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setId(String id)
    {
    }

    public String title;

    public boolean showTitle;

    public String widgetPrimaryColor;

    public String widgetSecondaryColor;

    public List<SharedAssetGraph> sharedGraphs;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setGraphs(Map<String, SharedAssetGraph> graphs)
    {
        if (graphs != null)
        {
            sharedGraphs = Lists.newArrayList(graphs.values());
        }
        else
        {
            sharedGraphs = Lists.newArrayList();
        }
    }

    private List<SharedAssetSelector> m_sharedSelectors;

    public List<SharedAssetSelector> getSharedSelectors()
    {
        selectorFixup();

        return m_sharedSelectors;
    }

    public void setSharedSelectors(List<SharedAssetSelector> selectors)
    {
        m_sharedSelectors = selectors;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setPrimaryColor(String primaryColor)
    {
        HubApplication.reportPatchCall(primaryColor);

        widgetPrimaryColor = primaryColor;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setSecondaryColor(String secondaryColor)
    {
        HubApplication.reportPatchCall(secondaryColor);

        widgetSecondaryColor = secondaryColor;
    }

    public final List<DashboardBannerSegment> bannerSegments = Lists.newArrayList();

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setLogoBase64(String logoBase64)
    {
        HubApplication.reportPatchCall(logoBase64);

        ensureBranding().logoBase64 = logoBase64;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setLogoLocation(LogoPlacement logoLocation)
    {
        HubApplication.reportPatchCall(logoLocation);

        switch (logoLocation)
        {
            case Left:
                ensureBranding().horizontalPlacement = HorizontalAlignment.Left;
                break;

            case Right:
                ensureBranding().horizontalPlacement = HorizontalAlignment.Right;
                break;
        }
    }

    private List<WidgetComposition> m_widgets;

    public List<WidgetComposition> getWidgets()
    {
        selectorFixup();

        return m_widgets;
    }

    public void setWidgets(List<WidgetComposition> widgets)
    {
        m_widgets = widgets;
    }

    private List<WidgetComposition> m_widgetLayout;

    // TODO: UPGRADE PATCH: Legacy fixup for renamed field
    public void setWidgetLayout(List<WidgetComposition> widgetLayout)
    {
        m_widgetLayout = widgetLayout;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setBranding(BrandingConfiguration branding)
    {
        HubApplication.reportPatchCall(branding);

        if (branding != null)
        {
            bannerSegments.clear();

            this.widgetPrimaryColor   = branding.primaryColor;
            this.widgetSecondaryColor = branding.secondaryColor;

            DashboardBannerSegment textSegment = new DashboardBannerSegment();
            textSegment.branding                     = new BrandingConfiguration();
            textSegment.branding.primaryColor        = branding.primaryColor;
            textSegment.branding.secondaryColor      = branding.secondaryColor;
            textSegment.branding.text                = this.title;
            textSegment.branding.horizontalPlacement = HorizontalAlignment.Right;
            textSegment.branding.verticalPlacement   = VerticalAlignment.Middle;
            textSegment.widthRatio                   = 1;

            if (StringUtils.isEmpty(branding.logoBase64))
            {
                bannerSegments.add(textSegment);
                return;
            }

            DashboardBannerSegment logoSegment = new DashboardBannerSegment();
            logoSegment.branding                   = new BrandingConfiguration();
            logoSegment.branding.logoBase64        = branding.logoBase64;
            logoSegment.branding.primaryColor      = branding.primaryColor;
            logoSegment.branding.verticalPlacement = VerticalAlignment.Middle;
            logoSegment.widthRatio                 = 1;

            if (branding.horizontalPlacement == HorizontalAlignment.Left)
            {
                logoSegment.branding.horizontalPlacement = HorizontalAlignment.Left;
                textSegment.branding.horizontalPlacement = HorizontalAlignment.Right;
                bannerSegments.add(logoSegment);
                bannerSegments.add(textSegment);
            }
            else
            {
                textSegment.branding.horizontalPlacement = HorizontalAlignment.Left;
                logoSegment.branding.horizontalPlacement = HorizontalAlignment.Right;
                bannerSegments.add(textSegment);
                bannerSegments.add(logoSegment);
            }
        }
    }

    private BrandingConfiguration ensureBranding()
    {
        if (bannerSegments.isEmpty())
        {
            DashboardBannerSegment segment = new DashboardBannerSegment();
            segment.branding = new BrandingConfiguration();
            bannerSegments.add(segment);
        }

        return bannerSegments.get(0).branding;
    }

    private void selectorFixup()
    {
        if (m_widgets == null)
        {
            Map<String, SharedAssetSelector>    selectors     = Maps.newHashMap();
            Multimap<String, AssetGraphBinding> graphBindings = HashMultimap.create();

            ModelSanitizerContext ctx = new ModelSanitizerContext.Simple(null)
            {
                @Override
                protected ModelSanitizerHandler.Target processInner(Object obj,
                                                                    ModelSanitizerHandler handler)
                {
                    AssetGraphSelectorWidgetConfiguration selectorWidget = Reflection.as(obj, AssetGraphSelectorWidgetConfiguration.class);
                    if (selectorWidget != null)
                    {
                        selectors.computeIfAbsent(selectorWidget.selectorId, selectorId ->
                        {
                            SharedAssetSelector selector = new SharedAssetSelector();
                            selector.id      = selectorWidget.selectorId;
                            selector.name    = "Selector";
                            selector.graphId = selectorWidget.selectorId;
                            return selector;
                        });
                    }

                    ControlPointWidgetConfiguration controlPointWidget = Reflection.as(obj, ControlPointWidgetConfiguration.class);
                    if (controlPointWidget != null)
                    {
                        updateBindingSelectorId(controlPointWidget.pointInput);
                    }

                    AggregationWidgetConfiguration aggregationWidget = Reflection.as(obj, AggregationWidgetConfiguration.class);
                    if (aggregationWidget != null)
                    {
                        updateBindingSelectorId(aggregationWidget.controlPointGroup.getPointInput());
                    }

                    return super.processInner(obj, handler);
                }
            };

            ctx.process(m_widgetLayout);

            graphBindings.forEach((graphId, binding) ->
                                  {
                                      if (selectors.get(graphId) != null)
                                      {
                                          binding.selectorId = binding.graphId;
                                      }
                                  });

            m_sharedSelectors = Lists.newArrayList(selectors.values());
            m_widgets         = m_widgetLayout;
        }
    }

    private void updateBindingSelectorId(AssetGraphBinding binding)
    {
        if (binding != null)
        {
            binding.selectorId = binding.graphId;
        }
    }
}
