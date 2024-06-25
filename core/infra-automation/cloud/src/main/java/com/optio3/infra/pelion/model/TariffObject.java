/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Provisioning
 * <p>Activate a subscriber and assign a tariff.<p><strong>Note&#58;</strong> If you want to perform bulk activations, you must use the Connectivity Management platform user interface.</p></p><p>You can use these endpoints to&#58;</p> <ul> <li>View a list of tariffs that you can assign to a stock subscriber</li> <li>Activate a stock subscriber and assign a tariff</li> </ul> <p><em>Stock</em> refers to subscribers (physical devices or virtual profiles) that are available ('in stock') and not yet activated. The Provisioning endpoints apply only to subscribers with a status of 'stock'. For any other subscriber state, such as 'active' or 'terminated', the endpoints return an error.</p> <p>You must provide the subscriber's <em>physicalId</em> as a parameter in the request. This value varies depending on the subscriber type&#58;</p><ul><li><em>Cellular</em> - use the ICCID</li><li><em>Non-IP</em> - use the Device EUI</li><li><em>Satellite</em> - use the IMEI</li></ul> <p><strong>Note&#58;</strong> You should allow between 24 and 48 hours for the network operator to process activation requests.</p>
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.pelion.model;

import java.math.BigDecimal;

public class TariffObject
{

    /**
     * The cost of line rental for the tariff expressed in pence (or in the smallest unit of the tariff currency).
     */
    public BigDecimal lineRentalPrice = null;
    /**
     * The tariff identifier, unique within your company.
     */
    public Integer    productSetID    = null;
    /**
     * The tariff name.
     */
    public String     tariffName      = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class TariffObject {\n");

        sb.append("    lineRentalPrice: ")
          .append(toIndentedString(lineRentalPrice))
          .append("\n");
        sb.append("    productSetID: ")
          .append(toIndentedString(productSetID))
          .append("\n");
        sb.append("    tariffName: ")
          .append(toIndentedString(tariffName))
          .append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private static String toIndentedString(java.lang.Object o)
    {
        if (o == null)
        {
            return "null";
        }
        return o.toString()
                .replace("\n", "\n    ");
    }
}
