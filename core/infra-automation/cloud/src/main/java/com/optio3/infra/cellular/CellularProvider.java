/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.cellular;

import com.optio3.cloud.client.deployer.model.DeployerCellularInfo;
import com.optio3.infra.directory.CredentialDirectory;
import org.apache.commons.lang3.StringUtils;

public enum CellularProvider
{
    Twilio
            {
                @Override
                public boolean mightBeCompatible(String imsi)
                {
                    if (StringUtils.startsWith(imsi, "310260"))
                    {
                        return true;
                    }

                    return false;
                }

                @Override
                public ICellularProviderHandler getHandler(CredentialDirectory credentials)
                {
                    return new TwilioHelper.Handler(credentials);
                }
            },
    TwilioSuperSim
            {
                @Override
                public boolean mightBeCompatible(String imsi)
                {
                    if (StringUtils.startsWith(imsi, "732123"))
                    {
                        return true;
                    }

                    return false;
                }

                @Override
                public ICellularProviderHandler getHandler(CredentialDirectory credentials)
                {
                    return new TwilioV2Helper.Handler(credentials);
                }
            },
    Pelion
            {
                @Override
                public boolean mightBeCompatible(String imsi)
                {
                    if (StringUtils.startsWith(imsi, "302760"))
                    {
                        return true;
                    }

                    return false;
                }

                @Override
                public ICellularProviderHandler getHandler(CredentialDirectory credentials)
                {
                    return new PelionHelper.Handler(credentials);
                }
            },
    Aeris
            {
                @Override
                public boolean mightBeCompatible(String imsi)
                {
                    if (StringUtils.startsWith(imsi, "310170"))
                    {
                        return true;
                    }

                    if (StringUtils.startsWith(imsi, "311882"))
                    {
                        return true;
                    }

                    return false;
                }

                @Override
                public ICellularProviderHandler getHandler(CredentialDirectory credentials)
                {
                    return null;
                }
            };

    public abstract boolean mightBeCompatible(String imsi);

    public abstract ICellularProviderHandler getHandler(CredentialDirectory credentials);

    public static boolean isValidICCID(String val)
    {
        if (val != null)
        {
            val = DeployerCellularInfo.trimFiller(val);

            switch (val.length())
            {
                case 19:
                case 20:
                    return true;
            }
        }

        return false;
    }
}
