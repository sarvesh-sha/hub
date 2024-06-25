/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu;

import java.util.Arrays;
import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.model.enums.MessageType;
import com.optio3.protocol.bacnet.model.enums.NetworkPriority;
import com.optio3.protocol.bacnet.model.pdu.application.ApplicationPDU;
import com.optio3.protocol.bacnet.model.pdu.network.NetworkMessagePDU;
import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.bacnet.error.BACnetDecodingException;
import com.optio3.serialization.ConditionalFieldSelector;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

//
// Version      1 octet
// Control      1 octet
// DNET         2 octets
// DLEN         1 octet
// DADR         Variable
// SNET         2 octets
// SLEN         1 octet
// SADR         Variable
// Hop Count    1 octet
// Message Type 1 octet
// Vendor ID    2 octets
// APDU         N octets
//
public final class NetworkPDU implements ConditionalFieldSelector,
                                         AutoCloseable
{
    private static final int    c_BACnet_Version = 1;
    private static final byte[] c_emptyAddress   = new byte[0];

    @SerializationTag(number = 0, width = 8)
    public byte version = c_BACnet_Version;

    @SerializationTag(number = 1, bitOffset = 7, width = 1)
    public boolean network_message;

    @SerializationTag(number = 1, bitOffset = 5, width = 1)
    public boolean destination_specifier;

    @SerializationTag(number = 1, bitOffset = 3, width = 1)
    public boolean source_specifier;

    //
    // The value of this bit corresponds to the data_expecting_reply parameter in the N-UNITDATA primitives.
    // * 1 indicates that a BACnet-Confirmed-Request-PDU, a segment of a BACnet-ComplexACK-PDU, or a network layer message expecting a reply is present.
    // * 0 indicates that other than a BACnet-Confirmed-Request-PDU, a segment of a BACnet-ComplexACK-PDU, or a network layer message expecting a reply is present.
    //
    @SerializationTag(number = 1, bitOffset = 2, width = 1)
    public boolean data_expecting_reply;

    @SerializationTag(number = 1, bitOffset = 0, width = 2)
    public NetworkPriority priority;

    @SerializationTag(number = 2)
    public Unsigned16 dnet;

    @SerializationTag(number = 3)
    public Unsigned8 dlen;

    @SerializationTag(number = 4)
    public byte[] dadr;

    @SerializationTag(number = 5)
    public Unsigned16 snet;

    @SerializationTag(number = 6)
    public Unsigned8 slen;

    @SerializationTag(number = 7)
    public byte[] sadr;

    @SerializationTag(number = 8)
    public Unsigned8 hop_count;

    @SerializationTag(number = 9)
    public MessageType message_type;

    private InputBuffer m_payload;

    //--//

    @Override
    public void close()
    {
        if (m_payload != null)
        {
            m_payload.close();
            m_payload = null;
        }
    }

    @Override
    public boolean shouldEncode(String fieldName)
    {
        switch (fieldName)
        {
            case "dnet":
            case "dlen":
            case "hop_count":
                return destination_specifier;

            case "dadr":
                return destination_specifier && (dlen != null && dlen.unbox() > 0);

            case "snet":
            case "slen":
                return source_specifier;

            case "sadr":
                return source_specifier && (slen != null && slen.unbox() > 0);

            case "message_type":
                return network_message;
        }

        return true;
    }

    @Override
    public boolean shouldDecode(String fieldName)
    {
        switch (fieldName)
        {
            case "dnet":
            case "dlen":
            case "hop_count":
                return destination_specifier;

            case "dadr":
                return destination_specifier && (dlen != null && dlen.unboxUnsigned() != 0);

            case "snet":
            case "slen":
                return source_specifier;

            case "sadr":
                return source_specifier && (slen != null && slen.unboxUnsigned() != 0);

            case "message_type":
                return network_message;
        }

        return true;
    }

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "dadr":
                buffer.emit(dadr);
                return true;

            case "sadr":
                buffer.emit(sadr);
                return true;
        }

        return false;
    }

    @Override
    public Optional<Object> provideValue(String fieldName,
                                         InputBuffer buffer)
    {
        switch (fieldName)
        {
            case "dadr":
                return Optional.of(buffer.readByteArray(dlen.unboxUnsigned()));

            case "sadr":
                return Optional.of(buffer.readByteArray(slen.unboxUnsigned()));
        }

        return Optional.empty();
    }

    //--//

    public BACnetAddress getSourceAddress()
    {
        if (!source_specifier)
        {
            return null;
        }

        return fromArray(snet, sadr);
    }

    public void setSourceAddress(BACnetAddress bacnetAddress)
    {
        if (bacnetAddress != null)
        {
            source_specifier = true;
            snet             = bacnetAddress.network_number;
            sadr             = bacnetAddress.mac_address != null ? bacnetAddress.mac_address : c_emptyAddress;
            slen             = Unsigned8.box(sadr.length);
        }
    }

    public BACnetAddress getDestinationAddress()
    {
        if (!destination_specifier)
        {
            return null;
        }

        return fromArray(dnet, dadr);
    }

    public void setDestinationAddress(BACnetAddress bacnetAddress)
    {
        if (bacnetAddress != null)
        {
            destination_specifier = true;
            dnet                  = bacnetAddress.network_number;
            dadr                  = bacnetAddress.mac_address != null ? bacnetAddress.mac_address : c_emptyAddress;
            dlen                  = Unsigned8.box(dadr.length);
        }
    }

    private static BACnetAddress fromArray(Unsigned16 networkNumber,
                                           byte[] address)
    {
        BACnetAddress addr = new BACnetAddress();
        addr.network_number = networkNumber;
        addr.mac_address    = address != null ? Arrays.copyOf(address, address.length) : null;
        return addr;
    }

    //--//

    public static NetworkPDU decode(InputBuffer ib)
    {
        byte b = ib.peekNextByte();
        if (b != c_BACnet_Version)
        {
            throw BACnetDecodingException.newException("Encountered unknown Network PDU version: %d", b);
        }

        NetworkPDU pdu = new NetworkPDU();

        SerializationHelper.read(ib, pdu);

        pdu.m_payload = ib.readNestedBlock(ib.remainingLength());

        return pdu;
    }

    public void encode(OutputBuffer buffer)
    {
        SerializationHelper.write(buffer, this);
    }

    public byte[] getPayload()
    {
        m_payload.setPosition(0);

        return m_payload.readByteArray(m_payload.size());
    }

    public ApplicationPDU decodeApplicationHeader()
    {
        m_payload.setPosition(0);

        return ApplicationPDU.decodeHeader(m_payload);
    }

    public NetworkMessagePDU decodeNetworkMessageHeader()
    {
        m_payload.setPosition(0);

        return NetworkMessagePDU.decode(message_type, m_payload);
    }
}
