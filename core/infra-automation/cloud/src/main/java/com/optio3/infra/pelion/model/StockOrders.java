/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Stock Order
 * Order stock and view your company's order history.<p>You can use these endpoints to place orders and retrieve&#58;<ul> <li>Your company's order history</li> <li>The details of a specific order</li> <li>A list of operators your company can order stock from</li> <li>A list of the type of stock your company can order from each operator</li> <li>A spreadsheet listing every subscriber included in a specific order</li> </ul>
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.pelion.model;

import java.time.ZonedDateTime;

public class StockOrders
{

    /**
     * The recipient's postal address.
     */
    public String        address         = null;
    /**
     * The date and time at which the order was placed.
     */
    public ZonedDateTime dateCreated     = null;
    /**
     * The delivery method.
     */
    public String        method          = null;
    /**
     * The recipient's name.
     */
    public String        name            = null;
    /**
     * The operator's name.
     */
    public String        network         = null;
    /**
     * The order's unique identifier.
     */
    public String        orderId         = null;
    /**
     * The number of stock items in the order.
     */
    public Integer       quantity        = null;
    /**
     * The status of the order.
     */
    public String        status          = null;
    /**
     * The tracking number
     */
    public String        trackingNumber  = null;
    /**
     * The unique identifier of the user who placed the order.
     */
    public Integer       userRequestedId = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class StockOrders {\n");

        sb.append("    address: ")
          .append(toIndentedString(address))
          .append("\n");
        sb.append("    dateCreated: ")
          .append(toIndentedString(dateCreated))
          .append("\n");
        sb.append("    method: ")
          .append(toIndentedString(method))
          .append("\n");
        sb.append("    name: ")
          .append(toIndentedString(name))
          .append("\n");
        sb.append("    network: ")
          .append(toIndentedString(network))
          .append("\n");
        sb.append("    orderId: ")
          .append(toIndentedString(orderId))
          .append("\n");
        sb.append("    quantity: ")
          .append(toIndentedString(quantity))
          .append("\n");
        sb.append("    status: ")
          .append(toIndentedString(status))
          .append("\n");
        sb.append("    trackingNumber: ")
          .append(toIndentedString(trackingNumber))
          .append("\n");
        sb.append("    userRequestedId: ")
          .append(toIndentedString(userRequestedId))
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
