/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import com.optio3.cloud.client.deployer.model.DeployerCellularInfo;
import com.optio3.infra.cellular.CellularProvider;

public class DeploymentHostDetails
{
    public DeployerCellularInfo cellular;

    public CellularProvider provider;
    public String           providerId;
    public String           providerPlan;

    //--//

    public boolean shouldUpdateCellular()
    {
        if (cellular == null)
        {
            return false;
        }

        if (provider == null)
        {
            return true;
        }

        //
        // Wrong provider?
        //
        return !provider.mightBeCompatible(cellular.modemIMSI);
    }

    public void update(DeployerCellularInfo cellular)
    {
        if (this.cellular == null)
        {
            this.cellular = cellular;
        }
        else if (cellular != null)
        {
            this.cellular.update(cellular.modemIMSI, cellular.modemIMEI, cellular.getModemICCID());
        }
    }
}
