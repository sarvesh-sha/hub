/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.obdii.PgnMessageType;

@JsonTypeName("Ipn:Obdii:Pgn:SoftwareIdentification")
@PgnMessageType(pgn = 65242, littleEndian = true)
public class SoftwareIdentification extends BaseSysPgnObjectModel
{
    /* TODO: PGN fields

Got J1939 message: 65242 : Software Identification : SOFT
  Number of Software Identification Fields: 1 byte | 1 | 1 step/bit | 0 | 0 to 250 steps | 0 to 125

         Number of software identification designators represented in the software identification parameter group.

  Software Identification: Variable - up to 200 bytes followed by an "*" delimiter | 2-N | ASCII | 0 | 0 to 255 per byte |

         Software identification of an electronic module.  As an example, this parameter may be represented with ASCII characters MMDDYYaa where MM is the month, DD is the day, YY is the year, and aa is the revision number.

           NOTE The ASCII character Ò*Ó is reserved as a delimiter.

     */

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "SoftwareIdentification";
    }
}
