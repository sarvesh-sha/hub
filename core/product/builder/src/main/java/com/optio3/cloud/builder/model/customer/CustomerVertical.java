/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.cloud.builder.model.communication.EmailMessage;
import com.optio3.cloud.builder.model.communication.TextMessage;
import com.optio3.cloud.client.hub.model.InstanceConfiguration;
import com.optio3.cloud.model.IEnumDescription;
import com.optio3.infra.deploy.CommonDeployer;

public enum CustomerVertical implements IEnumDescription
{
    None("None - Only For Testing From Backup!")
            {
                @Override
                public boolean shouldApplyToInstance()
                {
                    return false;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationDoNothing.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                    // Nothing to do.
                }
            },

    CRE("Commercial Real Estate")
            {
                @Override
                public boolean shouldApplyToInstance()
                {
                    return true;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationForCRE.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                    // Nothing to do.
                }
            },

    Digineous("Digineous")
            {
                @Override
                public void fixupEmail(EmailMessage msg)
                {
                    super.fixupEmail(msg);

                    msg.from.substituteDomain("digineous.com");
                }

                @Override
                public boolean shouldApplyToInstance()
                {
                    return true;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationForDigineous.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                    // Nothing to do;
                }

                @Override
                public void fixupText(TextMessage msg)
                {
                    if (msg.senderId != null)
                    {
                        switch (msg.senderId)
                        {
                            case "DIGOEE":
                            case "DIGSCS":
                            case "DIGCBM":
                                break;

                            default:
                                msg.senderId = null;
                                break;
                        }
                    }
                }
            },

    MontageWalmart("Montage Walmart")
            {
                @Override
                public boolean shouldApplyToInstance()
                {
                    return true;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationForMontageWalmart.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                }
            },

    MerlinSolar("Solar Truck")
            {
                @Override
                public boolean shouldApplyToInstance()
                {
                    return true;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationForMerlinSolar.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                    // Nothing to do.
                }
            },

    Palfinger("Palfinger")
            {
                @Override
                public boolean shouldApplyToInstance()
                {
                    return true;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationForPalfinger.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                    // Nothing to do.
                }
            },

    StealthPower("Stealth Power")
            {
                @Override
                public boolean shouldApplyToInstance()
                {
                    return true;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationForStealthPower.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                    // Nothing to do.
                }
            },

    EPowerAmazon("ePower - Amazon")
            {
                @Override
                public boolean shouldApplyToInstance()
                {
                    return true;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationForEPowerAmazon.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                }
            },

    DigitalMatter("DigitalMatter")
            {
                @Override
                public boolean shouldApplyToInstance()
                {
                    return true;
                }

                @Override
                public Class<? extends InstanceConfiguration> getHandler()
                {
                    return com.optio3.cloud.client.hub.model.InstanceConfigurationForDigitalMatter.class;
                }

                @Override
                public void fixupDeployer(CommonDeployer deployer)
                {
                    deployer.useStaticIp = true;
                }

                @Override
                public Map<Integer, Integer> extraTcpPortsToOpen()
                {
                    Map<Integer, Integer> map = Maps.newHashMap();
                    map.put(10000, 10000);
                    return map;
                }
            };

    private final String m_displayName;

    CustomerVertical(String displayName)
    {
        m_displayName = displayName;
    }

    @Override
    public String getDisplayName()
    {
        return m_displayName;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    public void fixupEmail(EmailMessage msg)
    {
    }

    public void fixupText(TextMessage msg)
    {
        msg.senderId = "Optio3";
    }

    public abstract boolean shouldApplyToInstance();

    public abstract Class<? extends com.optio3.cloud.client.hub.model.InstanceConfiguration> getHandler();

    public abstract void fixupDeployer(CommonDeployer deployer);

    public Map<Integer, Integer> extraTcpPortsToOpen()
    {
        return Collections.emptyMap();
    }

    public Map<Integer, Integer> extraUdpPortsToOpen()
    {
        return Collections.emptyMap();
    }
}
