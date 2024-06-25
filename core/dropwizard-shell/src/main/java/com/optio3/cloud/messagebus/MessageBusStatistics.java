/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

public class MessageBusStatistics
{
    public int sessions;

    public int  packetTx;
    public long packetTxBytes;
    public long packetTxBytesResent;
    public int  messageTx;

    public int  packetRx;
    public long packetRxBytes;
    public long packetRxBytesResent;
    public int  messageRx;

    public MessageBusStatistics copy()
    {
        var res = new MessageBusStatistics();

        res.sessions = sessions;

        res.packetTx            = packetTx;
        res.packetTxBytes       = packetTxBytes;
        res.packetTxBytesResent = packetTxBytesResent;
        res.messageTx           = messageTx;

        res.packetRx            = packetRx;
        res.packetRxBytes       = packetRxBytes;
        res.packetRxBytesResent = packetRxBytesResent;
        res.messageRx           = messageRx;

        return res;
    }

    public synchronized void add(MessageBusStatistics stats)
    {
        if (stats != null)
        {
            sessions += stats.sessions;

            packetTx += stats.packetTx;
            packetTxBytes += stats.packetTxBytes;
            packetTxBytesResent += stats.packetTxBytesResent;
            messageTx += stats.messageTx;

            packetRx += stats.packetRx;
            packetRxBytes += stats.packetRxBytes;
            packetRxBytesResent += stats.packetRxBytesResent;
            messageRx += stats.messageRx;
        }
    }

    public synchronized void subtract(MessageBusStatistics stats)
    {
        if (stats != null)
        {
            sessions -= stats.sessions;

            packetTx -= stats.packetTx;
            packetTxBytes -= stats.packetTxBytes;
            packetTxBytesResent -= stats.packetTxBytesResent;
            messageTx -= stats.messageTx;

            packetRx -= stats.packetRx;
            packetRxBytes -= stats.packetRxBytes;
            packetRxBytesResent -= stats.packetRxBytesResent;
            messageRx -= stats.messageRx;
        }
    }
}
