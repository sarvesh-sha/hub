/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.deployer.model.DockerCompressedLayerDescription;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.deployer.model.DockerImageDescription;
import com.optio3.cloud.client.deployer.model.DockerLayerChunkCompact;
import com.optio3.cloud.client.deployer.model.DockerLayerChunkCompactWithMetadata;
import com.optio3.cloud.client.deployer.model.DockerLayerChunks;
import com.optio3.cloud.client.deployer.model.DockerLayerChunksWithMetadata;
import com.optio3.cloud.client.deployer.model.DockerLayerDescription;
import com.optio3.cloud.client.deployer.model.DockerPackageDescription;
import com.optio3.cloud.client.deployer.proxy.DeployerStatusApi;
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.infra.NetworkHelper;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.DockerImageDownloader;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.infra.docker.model.Image;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.LoggerFactory;
import com.optio3.stream.MemoryMappedHeap;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.io.IOUtils;

public class ImagePullSessionWorker
{
    private class ProgressReporter
    {
        private final int m_reportFrequency;

        private final long m_downloadTotal;
        private       long m_downloadCurrent;

        private String m_layerId;
        private long   m_transferTotal;
        private long   m_transferCurrent;

        private final Stopwatch m_st = Stopwatch.createStarted();
        private       int       m_reportNextTime;
        private       int       m_reportLastTime;
        private       int       m_reportChunks;
        private       long      m_reportSize;

        private ProgressReporter(int reportFrequency,
                                 long downloadTotal)
        {
            m_reportFrequency = reportFrequency * 1000;
            m_downloadTotal   = downloadTotal;

            m_reportNextTime = m_reportFrequency;
        }

        void adjustTransfer(long total)
        {
            m_downloadCurrent += total;
        }

        void setupTransfer(String layerId,
                           long total)
        {
            m_layerId         = layerId;
            m_transferTotal   = total;
            m_transferCurrent = 0;
        }

        void completeTransfer()
        {
            m_downloadCurrent += m_transferTotal;

            reportProgress("Layer '%s' fetched in %s (%,d bytes out of %,d)...", m_layerId, TimeUtils.toText(m_st.elapsed()), m_downloadCurrent, m_downloadTotal);
        }

        void report(int chunkSize)
        {
            m_transferCurrent += chunkSize;

            m_reportChunks++;
            m_reportSize += chunkSize;

            int elapsed = (int) m_st.elapsed(TimeUnit.MILLISECONDS);
            if (elapsed > m_reportNextTime)
            {
                int time = elapsed - m_reportLastTime;

                double speed           = (1000.0 * m_reportSize / Math.max(1, time));
                long   stillToDownload = m_downloadTotal - (m_downloadCurrent + m_transferCurrent);
                int    etaSeconds      = (int) Math.ceil(stillToDownload / Math.max(1, speed));

                reportProgress("Layer '%s' progress %.2f%% (%d chunks, avg. %,d bytes/chunk, speed %,d bytes/sec, ETA: %,d seconds)...",
                               m_layerId,
                               100.0 * m_transferCurrent / m_transferTotal,
                               m_reportChunks,
                               m_reportSize / m_reportChunks,
                               (int) speed,
                               etaSeconds);

                m_reportLastTime = elapsed;
                m_reportNextTime = elapsed + m_reportFrequency;
                m_reportChunks   = 0;
                m_reportSize     = 0;
            }
        }
    }

    private static class ChunkTracker
    {
        final Encryption.Sha1Hash                 hash;
        final long                                offset;
        final long                                size;
        final DockerImageDownloader.ChunkLocation existingBlob;

        ChunkTracker(Encryption.Sha1Hash hash,
                     long offset,
                     long size,
                     DockerImageDownloader.ChunkLocation existingBlob)
        {
            this.hash         = hash;
            this.offset       = offset;
            this.size         = size;
            this.existingBlob = existingBlob;
        }
    }

    private class ChunksPending
    {
        final DockerImageDownloader.Reservation   reservation;
        final AdaptiveChunk                       chunkHelper;
        final ProgressReporter                    progress;
        final DockerImageDownloader.ChunkLocation layerBlob;
        final List<ChunkTracker>                  pendingList = Lists.newArrayList();

        ChunksPending(DockerImageDownloader.Reservation reservation,
                      AdaptiveChunk chunkHelper,
                      ProgressReporter progress,
                      DockerImageDownloader.Layer layer) throws
                                                         IOException
        {
            this.reservation = reservation;
            this.chunkHelper = chunkHelper;
            this.progress    = progress;
            this.layerBlob   = layer.getBlobForWrite(false);
        }

        CompletableFuture<Boolean> push(ChunkTracker tracker) throws
                                                              Exception
        {
            if (tracker.existingBlob != null)
            {
                DockerImageDownloader.ChunkLocation blobDst = layerBlob.extract(tracker.offset, tracker.size);

                try (MemoryMappedHeap.SeekableInputOutputStream output = blobDst.openForWrite())
                {
                    try (InputStream input = tracker.existingBlob.openForRead())
                    {
                        IOUtils.copyLarge(input, output, 0, tracker.size);
                    }
                }

                return wrapAsync(true);
            }

            long size = 0;
            for (ChunkTracker previousTracker : pendingList)
            {
                size += previousTracker.size;
            }

            if (tracker.size + size > chunkHelper.size())
            {
                if (!await(flush()))
                {
                    return wrapAsync(false);
                }
            }

            pendingList.add(tracker);

            return wrapAsync(true);
        }

        CompletableFuture<Boolean> flush() throws
                                           Exception
        {
            if (pendingList.size() > 1)
            {
                List<Encryption.Sha1Hash> hashes = CollectionUtils.transformToList(pendingList, (tracker) -> tracker.hash);

                chunkHelper.start();

                List<byte[]> buffers = await(callWithRetries("Fetch File Chunks", (proxy) -> proxy.fetchLayerChunks(m_image, hashes)));

                chunkHelper.stop();

                if (buffers != null)
                {
                    for (int i = 0; i < hashes.size(); i++)
                    {
                        ChunkTracker tracker = pendingList.get(0); // We remove the entries as they get processed, so the next one is always at index zero.
                        byte[]       buf     = buffers.get(i);

                        if (buf == null)
                        {
                            break;
                        }

                        DockerImageDownloader.ChunkLocation blobDst = layerBlob.extract(tracker.offset, tracker.size);

                        try (MemoryMappedHeap.SeekableInputOutputStream output = blobDst.openForWrite())
                        {
                            output.write(buf);

                            if (m_shutdownCheck.call())
                            {
                                return wrapAsync(null);
                            }

                            progress.report(buf.length);
                        }

                        reservation.registerLayerChunk(tracker.hash, blobDst);

                        pendingList.remove(0);
                    }
                }
            }

            for (ChunkTracker tracker : pendingList)
            {
                if (!await(fetchLargeChunk(tracker)))
                {
                    return wrapAsync(false);
                }
            }

            pendingList.clear();

            return wrapAsync(true);
        }

        private CompletableFuture<Boolean> fetchLargeChunk(ChunkTracker chunk) throws
                                                                               Exception
        {
            DockerImageDownloader.ChunkLocation blobDst = layerBlob.extract(chunk.offset, chunk.size);

            try (MemoryMappedHeap.SeekableInputOutputStream output = blobDst.openForWrite())
            {
                for (long offset = 0; offset < chunk.size; )
                {
                    final long offsetForChunk = offset;
                    final int  chunkSize      = (int) Math.min(chunkHelper.size(), chunk.size - offsetForChunk);

                    chunkHelper.start();

                    byte[] buf = await(callWithRetries("Fetch File Chunk", (proxy) -> proxy.fetchLayerChunk(m_image, chunk.hash, offsetForChunk, chunkSize)));
                    if (buf == null)
                    {
                        reportProgress("Abandoning download, chunk %s missing from server...", chunk.hash);
                        return wrapAsync(false);
                    }

                    if (buf.length != chunkSize)
                    {
                        reportProgress("Abandoning download, chunk %s returned by server is incorrect size (%d, was expecting %d)", chunk.hash, buf.length, chunkSize);
                        return wrapAsync(false);
                    }

                    chunkHelper.stop();

                    output.write(buf);

                    offset += buf.length;

                    if (m_shutdownCheck.call())
                    {
                        return wrapAsync(null);
                    }

                    progress.report(buf.length);
                }

                reservation.registerLayerChunk(chunk.hash, blobDst);
            }

            return wrapAsync(true);
        }
    }

    //--//

    private final boolean             m_useIncremental;
    private final DeployerApplication m_app;
    private final String              m_image;
    private final Callable<Boolean>   m_shutdownCheck;
    private final Consumer<String>    m_addLine;
    private final Consumer<String>    m_setImageSha;

    public ImagePullSessionWorker(DeployerApplication app,
                                  String image,
                                  Callable<Boolean> shutdownCheck,
                                  Consumer<String> addLine,
                                  Consumer<String> setImageSha)
    {
        boolean useIncremental;

        DockerImageArchitecture arch = FirmwareHelper.architecture();
        if (arch != null && arch.isArm32())
        {
            if (NetworkHelper.isCellularConnection())
            {
                useIncremental = true;
            }
            else
            {
                // Not on a cellular connection, use fast download.
                useIncremental = false;
            }
        }
        else
        {
            useIncremental = false;
        }

        m_app            = app;
        m_image          = image;
        m_shutdownCheck  = shutdownCheck;
        m_addLine        = addLine;
        m_setImageSha    = setImageSha;
        m_useIncremental = useIncremental;
    }

    CompletableFuture<Boolean> execute()
    {
        try
        {
            String imageSha = await(callWithRetries("Get Image Sha", (proxy) -> proxy.getDockerImageSha(m_image)));
            if (imageSha == null)
            {
                // Unknown image...
                return wrapAsync(false);
            }

            DockerImageIdentifier sourceId = new DockerImageIdentifier(imageSha);
            DockerImageIdentifier targetId = new DockerImageIdentifier(m_image);

            try (DockerHelper helper = new DockerHelper(null))
            {
                Image img = helper.inspectImageOrNull(new DockerImageIdentifier(imageSha));
                if (img != null)
                {
                    helper.tagImage(sourceId, targetId);
                    m_setImageSha.accept(img.id);
                    return wrapAsync(true);
                }
            }

            //
            // Step 1: get the package descriptor.
            //
            Stopwatch st = Stopwatch.createStarted();

            reportProgress("Getting descriptor for '%s'", m_image);
            DockerPackageDescription pkgDesc = await(callWithRetries("Describe Image", (proxy) -> proxy.describeDockerTarball(m_image)));
            if (pkgDesc == null)
            {
                // Unknown image...
                return wrapAsync(false);
            }

            DockerImageDownloader dl = m_app.getServiceNonNull(DockerImageDownloader.class);
            try (DockerImageDownloader.Reservation reservation = await(dl.acquire()))
            {
                DockerImageDownloader.PackageOfImages pkg = reservation.createPackage(pkgDesc.metadata, pkgDesc.repositories);

                //
                // Step 2: collect all needed layers.
                //
                if (m_useIncremental)
                {
                    if (!await(fetchIncrementalLayers(pkgDesc, reservation, pkg, targetId)))
                    {
                        return wrapAsync(false);
                    }
                }
                else
                {
                    if (!await(fetchWholeLayers(pkgDesc, reservation, pkg)))
                    {
                        return wrapAsync(false);
                    }
                }

                //
                // Step 3: convert layers into usable image.
                //
                generateImage(pkg, sourceId, targetId);
            }

            reportProgress("Download completed in %s", st);

            m_app.flushHeartbeat(false);

            return wrapAsync(true);
        }
        catch (Throwable t)
        {
            reportProgress("Failed due to %s", LoggerFactory.convertStackTraceToString(t));
            return wrapAsync(false);
        }
    }

    private void generateImage(DockerImageDownloader.PackageOfImages pkg,
                               DockerImageIdentifier sourceId,
                               DockerImageIdentifier targetId) throws
                                                               IOException
    {
        try (FileSystem.TmpFileHolder tmp = FileSystem.createTempFile("docker", ".pkg.tar"))
        {
            pkg.emit(tmp.get());

            reportProgress("Importing image '%s'...", m_image);
            try (DockerHelper helper = new DockerHelper(null))
            {
                try (FileInputStream input = new FileInputStream(tmp.get()))
                {
                    helper.setTimeout(20 * 60 * 1000);
                    helper.importImage(input, true);
                }
            }
        }

        try (DockerHelper helper = new DockerHelper(null))
        {
            Image img = helper.inspectImage(targetId);
            helper.tagImage(sourceId, targetId);
            m_setImageSha.accept(img.id);

            reportProgress("Imported image '%s' as %s...", m_image, img.id);
        }
    }

    private CompletableFuture<Boolean> fetchWholeLayers(DockerPackageDescription pkgDesc,
                                                        DockerImageDownloader.Reservation reservation,
                                                        DockerImageDownloader.PackageOfImages pkg) throws
                                                                                                   Exception
    {
        Set<String>                                   existingDiffIds = DockerImageDownloader.enumerateDiffIDs();
        AdaptiveChunk                                 chunkHelper     = new AdaptiveChunk();
        Map<String, DockerCompressedLayerDescription> missingLayers   = Maps.newHashMap();
        long                                          downloadSize    = 0;

        for (DockerImageDescription imageDesc : pkgDesc.images)
        {
            pkg.addImageDetails(imageDesc.fileName, imageDesc.details);

            for (String diffId : imageDesc.diffIdsToLayers.keySet())
            {
                if (!existingDiffIds.contains(diffId))
                {
                    //
                    // Get the layer descriptor for missing layers.
                    //
                    String                           layerId   = imageDesc.diffIdsToLayers.get(diffId);
                    DockerCompressedLayerDescription layerDesc = await(callWithRetries("Describe Layer", (proxy) -> proxy.describeDockerCompressedLayerTarball(m_image, layerId)));

                    missingLayers.put(layerId, layerDesc);

                    downloadSize += layerDesc.sizeCompressed;
                }
            }
        }

        reportProgress("Initiating download of up to %,d bytes for '%s'...", downloadSize, m_image);

        ProgressReporter progress = new ProgressReporter(30, downloadSize);

        for (String layerId : missingLayers.keySet())
        {
            DockerCompressedLayerDescription layerDesc = missingLayers.get(layerId);

            progress.setupTransfer(layerId, layerDesc.sizeCompressed);

            DockerImageDownloader.Layer layer = reservation.findLayer(layerId);
            if (layer != null)
            {
                reportProgress("Layer '%s' already present, recycling %,d bytes...", layerId, layerDesc.size);
            }
            else
            {
                reportProgress("Layer '%s' missing (%,d bytes, %,d bytes compressed), fetching...", layerId, layerDesc.size, layerDesc.sizeCompressed);

                layer = reservation.prepareNewLayer(layerId, layerDesc.version, layerDesc.json, layerDesc.size, layerDesc.sizeCompressed);
                {
                    //
                    // Fetch contents of each missing layer.
                    //
                    DockerImageDownloader.ChunkLocation blob = layer.getBlobForWrite(true);
                    try (MemoryMappedHeap.SeekableInputOutputStream output = blob.openForWrite())
                    {
                        final long length = layerDesc.sizeCompressed;

                        for (long offset = 0; offset < length; )
                        {
                            final long offsetForChunk = offset;
                            final int  chunkSize      = (int) Math.min(chunkHelper.size(), length - offsetForChunk);

                            chunkHelper.start();

                            byte[] buf = await(callWithRetries("Fetch Chunk", (proxy) -> proxy.fetchCompressedLayerTarball(m_image, layerId, offsetForChunk, chunkSize)));

                            chunkHelper.stop();

                            output.write(buf);

                            offset += buf.length;

                            if (m_shutdownCheck.call())
                            {
                                return wrapAsync(false);
                            }

                            progress.report(buf.length);
                        }
                    }
                }
                reservation.registerLayer(layer);
            }

            progress.completeTransfer();

            pkg.linkLayer(layer);
        }

        return wrapAsync(true);
    }

    private CompletableFuture<Boolean> fetchIncrementalLayers(DockerPackageDescription pkgDesc,
                                                              DockerImageDownloader.Reservation reservation,
                                                              DockerImageDownloader.PackageOfImages pkg,
                                                              DockerImageIdentifier targetId) throws
                                                                                              Exception
    {
        AdaptiveChunk                       chunkHelper   = new AdaptiveChunk();
        Map<String, DockerLayerDescription> missingLayers = Maps.newHashMap();
        long                                downloadSize  = 0;

        {
            reportProgress("Analyzing existing images...");
            var report = reservation.analyzeAllImages(targetId.getRegistryAddress());
            for (String failure : report.failures)
            {
                reportProgress("WARN: %s", failure);
            }

            for (DockerImageDownloader.PackageOfImages pkgFound : report.newPackages)
            {
                reportProgress("Computing hashes for %d images...", pkgFound.images.size());
                int hashCount = pkgFound.processAllChunks();
                reportProgress("Processed %d hashes.", hashCount);
            }
        }

        Set<String> existingDiffIds = DockerImageDownloader.enumerateDiffIDs();

        for (DockerImageDescription imageDesc : pkgDesc.images)
        {
            pkg.addImageDetails(imageDesc.fileName, imageDesc.details);

            for (String diffId : imageDesc.diffIdsToLayers.keySet())
            {
                if (!existingDiffIds.contains(diffId))
                {
                    //
                    // Step 2: get the layer descriptor for missing layers.
                    //
                    String                 layerId   = imageDesc.diffIdsToLayers.get(diffId);
                    DockerLayerDescription layerDesc = await(callWithRetries("Describe Layer", (proxy) -> proxy.describeDockerLayerTarball(m_image, layerId)));

                    missingLayers.put(layerId, layerDesc);

                    downloadSize += layerDesc.size;
                }
            }
        }

        reportProgress("Initiating download of %,d bytes for '%s'...", downloadSize, m_image);

        ProgressReporter progress = new ProgressReporter(30, downloadSize);

        for (String layerId : missingLayers.keySet())
        {
            DockerLayerDescription layerDesc = missingLayers.get(layerId);

            DockerImageDownloader.Layer layer = reservation.findLayer(layerId);
            if (layer != null)
            {
                reportProgress("Layer '%s' already present, recycling %,d bytes...", layerId, layerDesc.size);

                progress.adjustTransfer(layerDesc.size);
            }
            else
            {
                reportProgress("Layer '%s' has %,d bytes, fetching files details...", layerId, layerDesc.size);

                DockerLayerChunksWithMetadata chunksBlob       = await(callWithRetries("Describe Layer Files", (proxy) -> proxy.describeDockerLayerChunksWithMetadata(m_image, layerId)));
                int                           chunksSkipped    = 0;
                long                          bytesSkipped     = 0;
                int                           chunksToDownload = 0;
                long                          bytesToDownload  = 0;

                List<ChunkTracker> worklist = Lists.newArrayList();
                long               offset   = 0;

                for (DockerLayerChunkCompactWithMetadata chunk : chunksBlob.decode())
                {
                    DockerImageDownloader.ChunkLocation blob = reservation.findLayerChunk(chunk.hash);
                    if (blob == null)
                    {
                        if (chunk.hasSubstructure)
                        {
                            DockerLayerChunks subchunksBlob = await(callWithRetries("Describe Layer SubFiles", (proxy) -> proxy.describeDockerLayerSubchunks(m_image, layerId, chunk.hash)));

                            for (DockerLayerChunkCompact subchunk : subchunksBlob.decode())
                            {
                                if (subchunk.hash != null)
                                {
                                    ChunkTracker tracker = new ChunkTracker(subchunk.hash, offset, subchunk.size, reservation.findLayerChunk(subchunk.hash));

                                    if (tracker.existingBlob == null)
                                    {
                                        chunksToDownload += 1;
                                        bytesToDownload += subchunk.size;
                                    }
                                    else
                                    {
                                        chunksSkipped += 1;
                                        bytesSkipped += subchunk.size;
                                    }

                                    worklist.add(tracker);
                                    offset += tracker.size;
                                }
                            }
                        }
                        else
                        {
                            ChunkTracker tracker = new ChunkTracker(chunk.hash, offset, chunk.size, null);

                            chunksToDownload += 1;
                            bytesToDownload += chunk.size;

                            worklist.add(tracker);
                            offset += tracker.size;
                        }
                    }
                    else
                    {
                        ChunkTracker tracker = new ChunkTracker(chunk.hash, offset, chunk.size, blob);

                        chunksSkipped += 1;
                        bytesSkipped += chunk.size;

                        worklist.add(tracker);
                        offset += tracker.size;
                    }
                }

                reportProgress("Layer '%s' missing %d chunks (%,d bytes), reusing %d chunks (%,d bytes)...", layerId, chunksToDownload, bytesToDownload, chunksSkipped, bytesSkipped);

                progress.adjustTransfer(bytesSkipped);
                progress.setupTransfer(layerId, bytesToDownload);

                layer = reservation.prepareNewLayer(layerId, layerDesc.version, layerDesc.json, layerDesc.size);
                {
                    ChunksPending chunksPending = new ChunksPending(reservation, chunkHelper, progress, layer);

                    for (ChunkTracker tracker : worklist)
                    {
                        if (!await(chunksPending.push(tracker)))
                        {
                            return wrapAsync(false);
                        }
                    }

                    if (!await(chunksPending.flush()))
                    {
                        return wrapAsync(false);
                    }
                }
                reservation.registerLayer(layer);

                progress.completeTransfer();
            }

            pkg.linkLayer(layer);
        }

        return wrapAsync(true);
    }

    private void reportProgress(String fmt,
                                Object... args)
    {
        m_addLine.accept(String.format(fmt, args));
    }

    private <T> CompletableFuture<T> callWithRetries(String text,
                                                     FunctionWithException<DeployerStatusApi, CompletableFuture<T>> callback) throws
                                                                                                                              Exception
    {
        while (true)
        {
            try
            {
                RpcClient client = await(m_app.getRpcClient(30, TimeUnit.SECONDS));

                DeployerStatusApi proxy = client.createProxy(WellKnownDestination.Service.getId(), null, DeployerStatusApi.class, 2, TimeUnit.MINUTES);

                T res = await(callback.apply(proxy));
                return wrapAsync(res);
            }
            catch (TimeoutException t1)
            {
                DeployerApplication.LoggerInstance.error("Failed to execute %s, due to timeout", text);
            }
            catch (Throwable t)
            {
                DeployerApplication.LoggerInstance.error("Failed to execute %s, due to %s", text, t);
            }

            await(sleep(30, TimeUnit.SECONDS));

            if (m_shutdownCheck.call())
            {
                throw Exceptions.newRuntimeException("Failed to execute %s, too many retries", text);
            }
        }
    }
}
