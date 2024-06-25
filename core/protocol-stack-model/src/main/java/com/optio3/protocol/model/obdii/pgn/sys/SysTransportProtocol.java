/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.obdii.PgnMessageType;

@JsonTypeName("Ipn:Obdii:Pgn:SysTransportProtocol")
@PgnMessageType(pgn = 60416, littleEndian = true)
public class SysTransportProtocol extends BaseSysPgnObjectModel
{
    /* TODO: PGN fields

Got J1939 message: 60416 : Transport Protocol - Connection Mgmt : TP.CM.xx
  Total Message Size (TP.CM_RTS):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Total Number of Packets (TP.CM_RTS):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Maximum Number of Packets (TP.CM_RTS):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number of the packeted message (TP.CM_RTS):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Number of Packets that can be sent (TP.CM_CTS):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Next Packet Number to be sent (TP.CM_CTS):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number of the packeted message (TP.CM_CTS):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Total Message Size (TP.CM_EndofMsgACK):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Total Number of Packets (TP.CM_EndofMsgACK):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number of the packeted message (TP.CM_EndofMsgACK):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Total Message Size (TP.CM_BAM):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Total Number of Packets (TP.CM_BAM):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number of the packeted message (TP.CM_BAM):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Connection Abort Reason:  |  |  |  |  |

         See document J1939-21 for SPN details.

  Parameter Group Number of packeted message (TP.CM_Conn_Abort):  |  |  |  |  |

         See document J1939-21 for SPN details.

  Control Byte (TP.CM):  |  |  |  |  |

         See document J1939-21 for SPN details.


     */

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "SysTransportProtocol";
    }
}
