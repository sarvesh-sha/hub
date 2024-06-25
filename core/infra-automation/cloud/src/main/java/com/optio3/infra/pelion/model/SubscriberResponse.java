/*
 * Copyright (C) 2017-2020, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.pelion.model;

import java.time.ZonedDateTime;

public class SubscriberResponse
{
    /**
     * The contract start date, or date and time.
     */
    public ZonedDateTime connectionDate = null;
    /**
     * The tariff contract length, in months.
     */
    public Integer       contractLength = null;
    /**
     * If the subscriber uses Circuit Switched Data (CSD), this field displays its data number.     If the subscriber does not use CSD, this field is null.
     */
    public String        dataNumber     = null;
    /**
     * The total amount of data the subscriber has sent and received during the current billing period, in bytes.
     */
    public Long          dataUsage      = null;
    /**
     * If the subscriber is an eUICC SIM, this field displays its EID.
     */
    public String        eid            = null;
    /**
     * The contract end date, or date and time.
     */
    public ZonedDateTime expiryDate     = null;
    /**
     * If the subscriber is a member of a group, this field displays the unique group identifier. This is null for subscribers that are not in a group.
     */
    public Integer       groupId        = null;
    /**
     * The International Mobile Subscriber Identity number.
     */
    public String        imsi           = null;
    /**
     * <p>Indicates if the subscriber is active.</p><ul><li><em>True</em> - the subscriber is active.</li><li><em>False</em> - the subscriber is not active.</li></ul>
     */
    public Boolean       isActive       = null;
    /**
     * <p>Indicates if the subscriber is barred.</p><ul><li><em>True</em> - the subscriber is barred.</li><li><em>False</em> - the subscriber is not barred.</li></ul>
     */
    public Boolean       isBarred       = null;
    /**
     * The tariff recurring line rental fee in pence (or the smallest unit of the tariff currency).
     */
    public Integer       lineRental     = null;
    public NetworkState  networkState   = null;
    /**
     * The subscriber nickname.
     */
    public String        nickname       = null;
    /**
     * The unique operator identifier.
     */
    public String        operatorCode   = null;
    /**
     * The name of the operator the subscriber is associated with.
     */
    public String        operatorName   = null;
    /**
     * <p>The <em>physicalId</em> value depends on the subscriber type&#58;</p><ul><li><em>Cellular</em> - the ICCID</li><li><em>Non-IP</em> - the Device EUI</li><li><em>Satellite</em> - the IMEI</li></ul>
     */
    public String        physicalId     = null;
//    public RelatedItems  related        = null;
    /**
     * If the subscriber is an eUICC SIM or eSIM profile, this field displays its SMDP provider.
     */
    public String        smdpProvider   = null;
    /**
     * <p>The <em>subscriberId</em> value depends on the subscriber type&#58;</p><ul><li><em>Cellular</em> - the MSISDN</li><li><em>Non-IP</em> - the Device EUI</li><li><em>Satellite</em> - the Subscription ID</li></ul>
     */
    public String        subscriberId   = null;
    /**
     * Indicates whether the subscriber is physical or virtual.
     */
    public String        subscriberType = null;
    /**
     * The name of tariff the subscriber is on.
     */
    public String        tariffName     = null;
    /**
     * The date, or the date and time, at which the subscriber's contract was terminated. This is null for active subscribers.
     */
    public ZonedDateTime terminateDate  = null;
}
