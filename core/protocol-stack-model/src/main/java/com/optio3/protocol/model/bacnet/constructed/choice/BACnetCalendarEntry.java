/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.protocol.model.bacnet.BACnetDate;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateRange;
import com.optio3.serialization.SerializationTag;

public final class BACnetCalendarEntry extends Choice
{
    @SerializationTag(number = 0)
    public BACnetDate date;

    @SerializationTag(number = 1)
    public BACnetDateRange date_range;

    //
    // BACnetWeekNDay ::= OCTET STRING (SIZE (3))
    // 
    // - first octet month (1..14) where: 1 =January
    //      13 = odd months
    //      14 = even months
    //      X'FF' = any month
    // - second octet week-of-month(1..9) where: 1 = days numbered 1-7
    //      2  = days numbered 8-14
    //      3  = days numbered 15-21
    //      4 = days numbered 22-28
    //      5 = days numbered 29-31
    //      6 = last 7 days of this month
    //      7 = any of the 7 days prior to the last 7 days of this month
    //      8 = any of the 7 days prior to the last 14 days of this month
    //      9 = any of the 7 days prior to the last 21 days of this month
    //      X'FF' = any week of this month
    // - third octet day-of-week (1..7) where:
    //      1 = Monday
    //      7 = Sunday
    //      X'FF' = any day of week
    //
    @SerializationTag(number = 2)
    public byte[] weekNDay;
}
