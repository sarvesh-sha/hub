/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.obdii.PgnMessageType;

@JsonTypeName("Ipn:Obdii:Pgn:SysTransportProtocol")
@PgnMessageType(pgn = 60160, littleEndian = true)
public class SysTransportProtocolDataTransfer extends BaseSysPgnObjectModel
{
    /* TODO: PGN fields

Got J1939 message: 60160 : Transport Protocol - Data Transfer : TP.DT
  Sequence Number (TP.DT):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Packetized Data (TP.DT):  |  |  |  |  |

         See document J1939-21 for SPN details.

     */

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "SysTransportProtocol";
    }
}
