/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.obdii.PgnMessageType;

@JsonTypeName("Ipn:Obdii:Pgn:SysAcknowledgmentMessage")
@PgnMessageType(pgn = 59392, littleEndian = true)
public class SysAcknowledgmentMessage extends BaseSysPgnObjectModel
{
    /* TODO: PGN fields

Got J1939 message: 59392 : Acknowledgment Message : ACKM
  Group Function Value (ACK):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number (ACK):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Group Function Value (NACK):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number (NACK):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Group Function Value (NACK_AD):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number  (NACK_AD):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Group Function Value (NACK_Busy):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number (NACK_Busy):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Control Byte (ACKM):  |  |  |  |  |

         See document J1939-21 for SPN details.

     */

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "SysAcknowledgmentMessage";
    }
}
