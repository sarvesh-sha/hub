/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * api.godaddy.com
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 2.4.9
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.godaddy.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class DomainSummary
{

    /**
     * Authorization code for transferring the Domain
     */
    public String        authCode            = null;
    /**
     * Administrative contact for the domain registration
     */
    public Contact       contactAdmin        = null;
    /**
     * Billing contact for the domain registration
     */
    public Contact       contactBilling      = null;
    /**
     * Registration contact for the domain
     */
    public Contact       contactRegistrant   = null;
    /**
     * Technical contact for the domain registration
     */
    public Contact       contactTech         = null;
    /**
     * Date and time when this domain was created
     */
    public ZonedDateTime createdAt           = null;
    /**
     * Date and time when this domain was deleted
     */
    public ZonedDateTime deletedAt           = null;
    /**
     * Name of the domain
     */
    public String        domain              = null;
    /**
     * Unique identifier for this Domain
     */
    public Double        domainId            = null;
    /**
     * Whether or not the domain is protected from expiration
     */
    public Boolean       expirationProtected = null;
    /**
     * Date and time when this domain will expire
     */
    public ZonedDateTime expires             = null;
    /**
     * Whether or not the domain is on-hold by the registrar
     */
    public Boolean       holdRegistrar       = null;
    /**
     * Whether or not the domain is locked to prevent transfers
     */
    public Boolean       locked              = null;
    /**
     * Fully-qualified domain names for DNS servers
     */
    public List<String>  nameServers         = new ArrayList<String>();
    /**
     * Whether or not the domain has privacy protection
     */
    public Boolean       privacy             = null;
    /**
     * Whether or not the domain is configured to automatically renew
     */
    public Boolean       renewAuto           = null;
    /**
     * Date the domain must renew on
     */
    public ZonedDateTime renewDeadline       = null;
    /**
     * Whether or not the domain is eligble for renewal based on status
     */
    public Boolean       renewable           = null;
    /**
     * Processing status of the domain<br/><ul> <li><strong style='margin-left: 12px;'>ACTIVE</strong> - All is well</li> <li><strong style='margin-left: 12px;'>AWAITING*</strong> - System is waiting for the end-user to complete an action</li> <li><strong style='margin-left: 12px;'>CANCELLED*</strong> - Domain has been cancelled, and may or may not be reclaimable</li> <li><strong style='margin-left: 12px;'>CONFISCATED</strong> - Domain has been confiscated, usually for abuse, chargeback, or fraud</li> <li><strong style='margin-left: 12px;'>DISABLED*</strong> - Domain has been disabled</li> <li><strong style='margin-left: 12px;'>EXCLUDED*</strong> - Domain has been excluded from Firehose registration</li> <li><strong style='margin-left: 12px;'>EXPIRED*</strong> - Domain has expired</li> <li><strong style='margin-left: 12px;'>FAILED*</strong> - Domain has failed a required action, and the system is no longer retrying</li> <li><strong style='margin-left: 12px;'>HELD*</strong> - Domain has been placed on hold, and likely requires intervention from Support</li> <li><strong style='margin-left: 12px;'>LOCKED*</strong> - Domain has been locked, and likely requires intervention from Support</li> <li><strong style='margin-left: 12px;'>PARKED*</strong> - Domain has been parked, and likely requires intervention from Support</li> <li><strong style='margin-left: 12px;'>PENDING*</strong> - Domain is working its way through an automated workflow</li> <li><strong style='margin-left: 12px;'>RESERVED*</strong> - Domain is reserved, and likely requires intervention from Support</li> <li><strong style='margin-left: 12px;'>REVERTED</strong> - Domain has been reverted, and likely requires intervention from Support</li> <li><strong style='margin-left: 12px;'>SUSPENDED*</strong> - Domain has been suspended, and likely requires intervention from Support</li> <li><strong style='margin-left: 12px;'>TRANSFERRED*</strong> - Domain has been transferred out</li> <li><strong style='margin-left: 12px;'>UNKNOWN</strong> - Domain is in an unknown state</li> <li><strong style='margin-left: 12px;'>UNLOCKED*</strong> - Domain has been unlocked, and likely requires intervention from Support</li> <li><strong style='margin-left: 12px;'>UNPARKED*</strong> - Domain has been unparked, and likely requires intervention from Support</li> <li><strong style='margin-left: 12px;'>UPDATED*</strong> - Domain ownership has been transferred to another account</li> </ul>
     */
    public String        status              = null;
    /**
     * Whether or not the domain is protected from transfer
     */
    public Boolean       transferProtected   = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class DomainSummary {\n");

        sb.append("    authCode: ")
          .append(toIndentedString(authCode))
          .append("\n");
        sb.append("    contactAdmin: ")
          .append(toIndentedString(contactAdmin))
          .append("\n");
        sb.append("    contactBilling: ")
          .append(toIndentedString(contactBilling))
          .append("\n");
        sb.append("    contactRegistrant: ")
          .append(toIndentedString(contactRegistrant))
          .append("\n");
        sb.append("    contactTech: ")
          .append(toIndentedString(contactTech))
          .append("\n");
        sb.append("    createdAt: ")
          .append(toIndentedString(createdAt))
          .append("\n");
        sb.append("    deletedAt: ")
          .append(toIndentedString(deletedAt))
          .append("\n");
        sb.append("    domain: ")
          .append(toIndentedString(domain))
          .append("\n");
        sb.append("    domainId: ")
          .append(toIndentedString(domainId))
          .append("\n");
        sb.append("    expirationProtected: ")
          .append(toIndentedString(expirationProtected))
          .append("\n");
        sb.append("    expires: ")
          .append(toIndentedString(expires))
          .append("\n");
        sb.append("    holdRegistrar: ")
          .append(toIndentedString(holdRegistrar))
          .append("\n");
        sb.append("    locked: ")
          .append(toIndentedString(locked))
          .append("\n");
        sb.append("    nameServers: ")
          .append(toIndentedString(nameServers))
          .append("\n");
        sb.append("    privacy: ")
          .append(toIndentedString(privacy))
          .append("\n");
        sb.append("    renewAuto: ")
          .append(toIndentedString(renewAuto))
          .append("\n");
        sb.append("    renewDeadline: ")
          .append(toIndentedString(renewDeadline))
          .append("\n");
        sb.append("    renewable: ")
          .append(toIndentedString(renewable))
          .append("\n");
        sb.append("    status: ")
          .append(toIndentedString(status))
          .append("\n");
        sb.append("    transferProtected: ")
          .append(toIndentedString(transferProtected))
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
