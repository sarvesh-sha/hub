/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.validation.constraints.NotNull;

import com.google.common.base.Stopwatch;
import com.optio3.asyncawait.AsyncQueue;
import com.optio3.concurrency.AsyncSemaphore;
import com.optio3.lang.Unsigned8;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.model.enums.NetworkPriority;
import com.optio3.protocol.bacnet.model.pdu.NetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.TagContextForDecoding;
import com.optio3.protocol.bacnet.model.pdu.application.AbortPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ComplexAckPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ErrorPDU;
import com.optio3.protocol.bacnet.model.pdu.application.RejectPDU;
import com.optio3.protocol.bacnet.model.pdu.application.SegmentAckPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ServiceCommon;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.error.BACnetAbortedException;
import com.optio3.protocol.model.bacnet.error.BACnetFailedException;
import com.optio3.protocol.model.bacnet.error.BACnetNotSupportedException;
import com.optio3.protocol.model.bacnet.error.BACnetRejectedException;
import com.optio3.protocol.model.bacnet.error.BACnetSegmentationException;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;

public class ServiceRequestHandle<T extends ConfirmedServiceRequest, U extends ConfirmedServiceResponse<T>> implements IApplicationPduListener
{
    public static final Logger LoggerInstance = BACnetManager.LoggerInstance.createSubLogger(ServiceRequestHandle.class);

    private static final Duration         MinTimeout                      = Duration.of(400, ChronoUnit.MILLIS);
    private static final Duration         MaxTimeout                      = Duration.of(4, ChronoUnit.SECONDS);
    private static final Duration         MaxTimeoutPerOutstandingRequest = Duration.of(900, ChronoUnit.MILLIS);
    private static final TimeoutException SingletonForTimeout             = new TimeoutException();

    private final BACnetManager  m_manager;
    private final DeviceIdentity m_targetDevice;
    private final TimeoutSpec    m_timeoutSpec;
    private final T              m_request;
    private final Class<U>       m_responseClass;

    private final OutputBuffer                                                  m_payload;
    private final AsyncQueue<FunctionWithException<ServiceRequestResult<U>, U>> m_events = new AsyncQueue<>();
    private final Duration                                                      m_minTimeout;
    private final Duration                                                      m_maxTimeout;
    private final Duration                                                      m_maxTimeoutPerOutstandingRequest;

    private byte                                       m_invokeId;
    private CompletableFuture<ServiceRequestResult<U>> m_response;
    private int                                        m_windowSize;
    private int                                        m_segmentSize;
    private int                                        m_txLastSegmentAcknowledged = -1;
    private int                                        m_txLastSegmentSent         = -1;
    private OutputBuffer                               m_rxPayload;
    private int                                        m_rxLastSegmentAcknowledged = -1;

    ServiceRequestHandle(BACnetManager manager,
                         DeviceIdentity target,
                         @NotNull TimeoutSpec timeoutSpec,
                         Duration minTimeout,
                         Duration maxTimeout,
                         Duration maxTimeoutPerOutstandingRequest,
                         @NotNull T request,
                         @NotNull Class<U> responseClass)
    {
        m_manager       = manager;
        m_targetDevice  = target;
        m_timeoutSpec   = timeoutSpec;
        m_request       = request;
        m_responseClass = responseClass;

        m_minTimeout                      = minTimeout != null ? minTimeout : MinTimeout;
        m_maxTimeout                      = maxTimeout != null ? maxTimeout : MaxTimeout;
        m_maxTimeoutPerOutstandingRequest = maxTimeoutPerOutstandingRequest != null ? maxTimeoutPerOutstandingRequest : MaxTimeoutPerOutstandingRequest;

        m_payload = m_request.encodePayload();
    }

    public void cancel()
    {
        if (m_response != null)
        {
            m_response.cancel(false);
        }
    }

    //--//

    public CompletableFuture<ServiceRequestResult<U>> result()
    {
        if (m_response == null)
        {
            m_response = executeRequest();
        }

        return m_response;
    }

    private CompletableFuture<ServiceRequestResult<U>> executeRequest()
    {
        ServiceRequestResult<U> result = new ServiceRequestResult<>();

        result.requestCount = 1;

        try
        {
            if (await(m_targetDevice.waitForResolution(m_timeoutSpec.timeout.toMillis(), TimeUnit.MILLISECONDS)) == null)
            {
                setFailure(result, Exceptions.newTimeoutException("Timeout while resolving device identity: %s", m_targetDevice));
            }
            else
            {
                Stopwatch swAcquire = Stopwatch.createStarted();

                try (AsyncSemaphore.Holder holderForNetwork = await(m_manager.acquireAccessToNetwork(m_targetDevice)))
                {
                    try (AsyncSemaphore.Holder holderForIP = await(m_manager.acquireAccessToTransport(m_targetDevice)))
                    {
                        result.waitForTransportAccess = (int) swAcquire.elapsed(TimeUnit.MILLISECONDS);

                        LoggerInstance.debugVerbose("[Device %s] Acquired access in %,dmsec", m_targetDevice, result.waitForTransportAccess);

                        Stopwatch swRequest = Stopwatch.createStarted();

                        try (BACnetManager.InvokeIdHolder holderForInvokeId = await(m_manager.acquireInvokeId(m_targetDevice)))
                        {
                            try (InvokeIdListener invokeListener = m_manager.registerForInvokeId(m_targetDevice, holderForInvokeId.get(), this))
                            {
                                m_invokeId = invokeListener.getInvokeId();

                                LoggerInstance.debugVerbose("[Device %s] %s/%02x ...",
                                                            m_targetDevice,
                                                            m_request.getClass()
                                                                     .getSimpleName(),
                                                            m_invokeId);

                                boolean fitsInOnePacket = (m_payload.size() <= safePayloadSize());

                                if (!fitsInOnePacket && !m_targetDevice.supportsReceiveSegmentation())
                                {
                                    result.failure = Exceptions.newGenericException(BACnetSegmentationException.class,
                                                                                    "Destination %s does not support segmentation, message too long (%d bytes)",
                                                                                    m_targetDevice,
                                                                                    m_payload.size());
                                }
                                else
                                {
                                    m_windowSize  = 1;
                                    m_segmentSize = safePayloadSize();

                                    while (true)
                                    {
                                        if (m_manager.isClosed())
                                        {
                                            setFailure(result, SingletonForTimeout);
                                            break;
                                        }

                                        // Increase the timeout based on the number of parallel requests.
                                        int      outstandingPermits = holderForNetwork.getOutstandingPermits();
                                        Duration timeout            = m_timeoutSpec.timeout;

                                        // Increase timeout with each attempt.
                                        timeout = TimeUtils.multiply(timeout, 1.0f + result.packetTxTimeouts * 0.5f);

                                        // Cap max timeout before multiplying by the neuxt of outstanding permits.
                                        timeout = TimeUtils.min(timeout, m_maxTimeoutPerOutstandingRequest);
                                        timeout = TimeUtils.multiply(timeout, outstandingPermits);

                                        // Crop timeout to be between Min and Max limits.
                                        timeout = TimeUtils.max(timeout, m_minTimeout);
                                        timeout = TimeUtils.min(timeout, m_maxTimeout);

                                        Duration timeLeft = m_timeoutSpec.capToDeadline(timeout);
                                        if (timeLeft == null)
                                        {
                                            dumpRequestInfo(result, timeout, swRequest, false, false);

                                            setFailure(result, m_timeoutSpec.deadlineException);
                                            break;
                                        }

                                        while ((m_txLastSegmentSent - m_txLastSegmentAcknowledged) < m_windowSize)
                                        {
                                            result.packetTx++;

                                            if (fitsInOnePacket)
                                            {
                                                result.packetTxBytes += sendSinglePacketRequest();
                                            }
                                            else
                                            {
                                                result.packetTxBytes += sendNextPacketRequest();
                                            }
                                        }

                                        try
                                        {
                                            FunctionWithException<ServiceRequestResult<U>, U> eventCallback = await(m_events.pull(), timeLeft.toNanos(), TimeUnit.NANOSECONDS);

                                            U msg = eventCallback.apply(result);
                                            if (msg != null)
                                            {
                                                Duration requestRoundtrip = swRequest.elapsed();

                                                result.value            = msg;
                                                result.requestRoundtrip = (int) requestRoundtrip.toMillis();

                                                //
                                                // Only update statistics if this was the first outstanding request for this device, to avoid skews.
                                                //
                                                if (outstandingPermits == 1 && requestRoundtrip.compareTo(m_maxTimeoutPerOutstandingRequest) <= 0)
                                                {
                                                    m_targetDevice.updateTiming(requestRoundtrip);
                                                }

                                                m_manager.recordNetworkRequestOutcome(m_targetDevice, true, false);
                                                dumpRequestInfo(result, timeLeft, swRequest, false, true);
                                                break;
                                            }
                                        }
                                        catch (TimeoutException e)
                                        {
                                            if (m_timeoutSpec.isPastDeadline())
                                            {
                                                dumpRequestInfo(result, timeout, swRequest, false, false);

                                                setFailure(result, m_timeoutSpec.deadlineException);
                                                break;
                                            }

                                            if (++result.packetTxTimeouts >= m_timeoutSpec.retries)
                                            {
                                                m_manager.recordNetworkRequestOutcome(m_targetDevice, false, true);
                                                dumpRequestInfo(result, timeout, swRequest, false, false);

                                                Exception failure;

                                                if (BACnetManager.uniqueTraces)
                                                {
                                                    failure = new TimeoutException();
                                                }
                                                else
                                                {
                                                    failure = SingletonForTimeout; // Use a singleton object, since timeouts don't have per-instance state.
                                                }

                                                setFailure(result, failure);
                                                break;
                                            }

                                            dumpRequestInfo(result, timeout, swRequest, true, false);

                                            m_txLastSegmentSent = m_txLastSegmentAcknowledged;

                                            // Sleep a little after a timeout.
                                            await(sleep(100, TimeUnit.MILLISECONDS));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception t)
        {
            result.failure = t;
        }

        m_targetDevice.updateNetworkStatistics(result);

        m_payload.close();

        return wrapAsync(result);
    }

    private void setFailure(ServiceRequestResult<U> result,
                            Exception failure)
    {
        result.failure        = failure;
        result.failureTarget  = m_targetDevice;
        result.failureContext = m_request.getClass();
    }

    private void dumpRequestInfo(ServiceRequestResult<U> perf,
                                 Duration timeout,
                                 Stopwatch sw,
                                 boolean gotTimeout,
                                 boolean gotResult)
    {
        if (LoggerInstance.isEnabled(Severity.Debug))
        {
            final Duration maxResponseTime = m_targetDevice.getMaxResponseTime();
            final Duration elapsed         = sw.elapsed();

            final String requestType = m_request.getClass()
                                                .getSimpleName();

            LoggerInstance.debug("[Device %s] %s/%02x | %s | timeout:%,d requestTime:%,d maxResponse:%,d retries:%d",
                                 m_targetDevice,
                                 requestType,
                                 m_invokeId,
                                 gotResult ? "SUCCESS" : gotTimeout ? "TIMEOUT" : "FAILURE",
                                 timeout.toMillis(),
                                 elapsed.toMillis(),
                                 maxResponseTime.toMillis(),
                                 perf.packetTxTimeouts);
        }
    }

    private int sendSinglePacketRequest()
    {
        m_txLastSegmentSent = 1;

        try (OutputBuffer ob = new OutputBuffer())
        {
            //
            // Prepare NPDU
            //
            try (NetworkPDU npdu = prepareNetworkPDU())
            {
                npdu.setDestinationAddress(m_targetDevice.getBACnetAddress());
                npdu.encode(ob);
            }

            //
            // Add APDU.
            //
            ConfirmedRequestPDU apdu = prepareRequestPDU(m_invokeId);

            if (m_targetDevice.supportsTransmitSegmentation())
            {
                apdu.segmentedResponseAccepted = true;
                apdu.setMaxSegmentsAccepted(ConfirmedRequestPDU.MaxSegment_64);
            }

            apdu.encodeHeader(ob);
            ob.emitNestedBlock(m_payload, 0, m_payload.size());

            return m_manager.sendDirect(m_targetDevice.getTransportAddress(), ob);
        }
    }

    private int sendNextPacketRequest()
    {
        int segmentNumber = ++m_txLastSegmentSent;
        int payloadOffset = segmentNumber * m_segmentSize;
        int payloadSize   = m_payload.size();

        if (payloadOffset >= payloadSize)
        {
            // Already sent all the segments.
            return 0;
        }

        int remainingPayload = payloadSize - payloadOffset;
        int payloadToSend    = Math.min(m_segmentSize, remainingPayload);

        try (OutputBuffer ob = new OutputBuffer())
        {
            NetworkPDU npdu = prepareNetworkPDU();
            npdu.setDestinationAddress(m_targetDevice.getBACnetAddress());
            npdu.encode(ob);

            //
            // Add APDU.
            //
            ConfirmedRequestPDU apdu = prepareRequestPDU(m_invokeId);
            apdu.segmentedMessage          = true;
            apdu.segmentedResponseAccepted = true;
            apdu.moreFollows               = payloadToSend != remainingPayload;
            apdu.sequenceNumber            = Unsigned8.box(segmentNumber);
            apdu.proposedWindowSize        = Unsigned8.box(1);
            apdu.setMaxSegmentsAccepted(ConfirmedRequestPDU.MaxSegment_64);
            apdu.encodeHeader(ob);
            ob.emitNestedBlock(m_payload, payloadOffset, payloadToSend);

            return m_manager.sendDirect(m_targetDevice.getTransportAddress(), ob);
        }
    }

    private int sendNextPacketResponse(int segmentNumber,
                                       int windowSize,
                                       boolean negativeAck)
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            NetworkPDU npdu = prepareNetworkPDU();
            npdu.setDestinationAddress(m_targetDevice.getBACnetAddress());
            npdu.encode(ob);

            //
            // Add APDU.
            //
            SegmentAckPDU apdu = new SegmentAckPDU(null);
            apdu.invokeId           = Unsigned8.box(m_invokeId);
            apdu.sequenceNumber     = Unsigned8.box(segmentNumber);
            apdu.proposedWindowSize = Unsigned8.box(windowSize);
            apdu.negativeAck        = negativeAck;

            apdu.encodeHeader(ob);

            return m_manager.sendDirect(m_targetDevice.getTransportAddress(), ob);
        }
    }

    private NetworkPDU prepareNetworkPDU()
    {
        NetworkPDU npdu = new NetworkPDU();
        npdu.data_expecting_reply = true;
        npdu.hop_count            = Unsigned8.box(255);
        npdu.priority             = NetworkPriority.Normal;
        return npdu;
    }

    private ConfirmedRequestPDU prepareRequestPDU(byte invokeId)
    {
        ConfirmedRequestPDU apdu = m_request.preparePCI();
        apdu.invokeId = Unsigned8.box(invokeId);
        apdu.setMaxApduLengthAccepted(ConfirmedRequestPDU.MaxAPDU_1476);
        return apdu;
    }

    private int safePayloadSize()
    {
        // Leave a bit of margin for the header.
        return m_targetDevice.getMaxApdu() - 64;
    }

    //--//

    @Override
    public void processRequestChunk(ServiceContext sc,
                                    ConfirmedRequestPDU pdu)
    {
        m_events.push((result) ->
                      {
                          m_targetDevice.markReceivedPacket(result, sc.packetLength);

                          throw new BACnetNotSupportedException("processRequestChunk not implemented yet!");
                      });
    }

    @Override
    public void processResponseChunk(ServiceContext sc,
                                     ComplexAckPDU pdu)
    {
        m_events.push((result) ->
                      {
                          m_targetDevice.markReceivedPacket(result, sc.packetLength);

                          if (m_rxPayload == null)
                          {
                              m_rxPayload = new OutputBuffer();
                          }

                          int sequenceNumber = Unsigned8.unboxOrDefault(pdu.sequenceNumber, (byte) 0);
                          int windowSize     = Unsigned8.unboxOrDefault(pdu.proposedWindowSize, (byte) 0);

                          if (LoggerInstance.isEnabled(Severity.Debug))
                          {
                              final String requestType = m_request.getClass()
                                                                  .getSimpleName();

                              LoggerInstance.debug("[Device %s] %s/%02x | processResponseChunk | %d/%d | %d | %s",
                                                   m_targetDevice,
                                                   requestType,
                                                   m_invokeId,
                                                   sequenceNumber,
                                                   m_rxLastSegmentAcknowledged,
                                                   windowSize,
                                                   pdu.moreFollows ? "<more>" : "<last>");
                          }

                          if (sequenceNumber == m_rxLastSegmentAcknowledged + 1)
                          {
                              sendNextPacketResponse(sequenceNumber, windowSize, false);

                              pdu.appendPayload(m_rxPayload);

                              m_rxLastSegmentAcknowledged++;
                          }
                          else
                          {
                              sendNextPacketResponse(m_rxLastSegmentAcknowledged, windowSize, true);
                          }

                          if (pdu.moreFollows)
                          {
                              return null;
                          }

                          ServiceCommon res = Reflection.newInstance(m_request.getChoice()
                                                                              .response());

                          TagContextForDecoding context = new TagContextForDecoding(res);

                          try (InputBuffer inputPayload = new InputBuffer(m_rxPayload))
                          {
                              context.decode(inputPayload);
                          }

                          m_rxPayload.close();
                          m_rxPayload = null;

                          return m_responseClass.cast(res);
                      });
    }

    @Override
    public void processSegmentAck(ServiceContext sc,
                                  SegmentAckPDU pdu)
    {
        m_events.push((result) ->
                      {
                          m_targetDevice.markReceivedPacket(result, sc.packetLength);

                          if (pdu.negativeAck)
                          {
                              throw new BACnetFailedException("Device segment NACK");
                          }

                          byte ackSegmentNumber = pdu.sequenceNumber.unbox();
                          LoggerInstance.debug("Acknowledged segment %d in message %d", ackSegmentNumber, pdu.invokeId);

                          for (int segmentNumber = m_txLastSegmentAcknowledged + 1; segmentNumber <= m_txLastSegmentSent; segmentNumber++)
                          {
                              byte segmentNumberRounded = (byte) segmentNumber;

                              if (segmentNumberRounded == ackSegmentNumber)
                              {
                                  m_txLastSegmentAcknowledged = segmentNumber;
                                  m_txLastSegmentSent         = segmentNumber;
                                  return null;
                              }
                          }

                          return null;
                      });
    }

    @Override
    public void processResponse(ServiceContext sc,
                                ConfirmedServiceResponse<?> res)
    {
        m_events.push((result) ->
                      {
                          m_targetDevice.markReceivedPacket(result, sc.packetLength);

                          if (res instanceof ConfirmedServiceResponse.NoDataReply)
                          {
                              return Reflection.newInstance(m_responseClass);
                          }

                          return m_responseClass.cast(res);
                      });
    }

    @Override
    public void processReject(ServiceContext sc,
                              RejectPDU pdu)
    {
        m_events.push((result) ->
                      {
                          m_targetDevice.markReceivedPacket(result, sc.packetLength);

                          throw new BACnetRejectedException(pdu.rejectReason);
                      });
    }

    @Override
    public void processAbort(ServiceContext sc,
                             AbortPDU pdu)
    {
        m_events.push((result) ->
                      {
                          m_targetDevice.markReceivedPacket(result, sc.packetLength);

                          throw new BACnetAbortedException(pdu.abortReason);
                      });
    }

    @Override
    public void processError(ServiceContext sc,
                             ErrorPDU pdu)
    {
        m_events.push((result) ->
                      {
                          m_targetDevice.markReceivedPacket(result, sc.packetLength);

                          throw Exceptions.newGenericException(BACnetFailedException.class, "Error with %s", pdu.errorChoice);
                      });
    }
}
