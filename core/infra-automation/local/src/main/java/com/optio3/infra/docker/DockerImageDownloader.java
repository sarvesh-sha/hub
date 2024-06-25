/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.docker;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.ws.rs.NotFoundException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.archive.TarBuilder;
import com.optio3.archive.TarWalker;
import com.optio3.archive.ZipWalker;
import com.optio3.cloud.client.deployer.model.DockerLayerChunkCompact;
import com.optio3.cloud.client.deployer.model.DockerLayerChunkCompactWithMetadata;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.concurrency.Executors;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.model.Image;
import com.optio3.infra.docker.model.ImageSummary;
import com.optio3.logging.Logger;
import com.optio3.stream.MemoryMappedHeap;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.ResourceAutoCleaner;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class DockerImageDownloader implements AutoCloseable
{
    private static final String c_blob_VERSION = "VERSION";
    private static final String c_blob_JSON    = "json";
    private static final String c_blob_TAR     = "layer.tar";
    private static final String c_manifest     = "manifest.json";
    private static final String c_repositories = "repositories";

    public static final Logger LoggerInstance = new Logger(DockerImageDownloader.class);

    public static class ManifestItem
    {
        public String   Config;
        public String[] RepoTags;
        public String[] Layers;
    }

    public static class ImageIdentity
    {
        public String                sha;
        public DockerImageIdentifier tag;
    }

    public static class ImageAnalysisReport
    {
        public final List<PackageOfImages> newPackages = Lists.newArrayList();
        public final List<String>          failures    = Lists.newArrayList();
    }

    public static class ImageDetails
    {
        public final String fileName;
        public final byte[] raw;
        public final Image  contents;

        public final Map<String, Layer> diffIdToLayer = Maps.newHashMap();

        private ImageDetails(String fileName,
                             byte[] raw) throws
                                         IOException
        {
            this.fileName = fileName;
            this.raw = raw;
            this.contents = DockerHelper.decodeJson(raw, Image.class);
        }

        private void emit(TarBuilder builder) throws
                                              IOException
        {
            builder.addAsBytes(null, fileName, raw, 0666);
        }
    }

    public class PackageOfImages
    {
        public String             imageSha;
        public byte[]             manifestRaw;
        public List<ManifestItem> manifests = Lists.newArrayList();

        public byte[] repositoriesRaw;

        public Map<String, ImageDetails> images = Maps.newHashMap();

        public Map<String, Layer> layers = Maps.newHashMap();

        //--//

        private void setManifests(byte[] raw) throws
                                              IOException
        {
            manifestRaw = raw;

            ManifestItem[] array = DockerHelper.decodeJson(raw, ManifestItem[].class);
            if (array != null)
            {
                for (ManifestItem manifestItem : array)
                {
                    manifests.add(manifestItem);
                }
            }
        }

        private void setRepositories(byte[] raw)
        {
            repositoriesRaw = raw;
        }

        //--//

        public int processAllChunks() throws
                                      Exception
        {
            int count = 0;

            for (DockerImageDownloader.Layer layer : layers.values())
            {
                List<DockerLayerChunkCompact> chunks = layer.getChunks();
                count += chunks.size();
            }

            return count;
        }

        public ImageDetails addImageDetails(String fileName,
                                            byte[] raw) throws
                                                        IOException
        {
            ImageDetails details = new ImageDetails(fileName, raw);

            images.put(fileName, details);

            return details;
        }

        public Layer findLayer(String layerId)
        {
            return layers.get(layerId);
        }

        public void linkLayer(Layer l)
        {
            layers.put(l.id, l);
        }

        public void emit(File target) throws
                                      IOException
        {
            try (FileOutputStream stream = new FileOutputStream(target))
            {
                try (TarBuilder builder = new TarBuilder(stream, false))
                {
                    for (Layer layer : layers.values())
                    {
                        layer.emit(builder);
                    }

                    for (ImageDetails imageDetails : images.values())
                    {
                        imageDetails.emit(builder);
                    }

                    builder.addAsBytes(null, c_manifest, manifestRaw, 0644);
                    builder.addAsBytes(null, c_repositories, repositoriesRaw, 0644);
                }
            }
        }

        public byte[] readFileChunk(Encryption.Sha1Hash hash) throws
                                                              IOException
        {
            ChunkLocation chunk = m_state.chunkLookup.get(hash);
            if (chunk == null)
            {
                return null;
            }

            return readFileChunk(chunk, 0, (int) chunk.getSize());
        }

        public byte[] readFileChunk(Encryption.Sha1Hash hash,
                                    long offset,
                                    int size) throws
                                              IOException
        {
            ChunkLocation chunk = m_state.chunkLookup.get(hash);
            if (chunk == null)
            {
                return null;
            }

            return readFileChunk(chunk, offset, size);
        }

        private byte[] readFileChunk(ChunkLocation chunk,
                                     long offset,
                                     int size) throws
                                               IOException
        {
            try (InputStream stream = m_state.heap.resource.sliceAsInputStream(chunk.m_offset, chunk.m_length))
            {
                if (stream.skip(offset) != offset)
                {
                    return null;
                }

                return IOUtils.readFully(stream, size);
            }
        }
    }

    public class Layer
    {
        private class ChunkDetails
        {
            private long offsetHeaderStart;
            private long offsetHeaderEnd;
            private long offsetEntryEnd;
            private long sizeContent;

            private String entryName;

            private DockerLayerChunkCompact       m_chunkHeader;
            private DockerLayerChunkCompact       m_chunkEntry;
            private List<DockerLayerChunkCompact> m_subChunks;

            //--//

            void getChunksWithMetadata(List<DockerLayerChunkCompactWithMetadata> lst)
            {
                lst.add(new DockerLayerChunkCompactWithMetadata(m_chunkHeader.hash, m_chunkHeader.size, false));

                if (m_chunkEntry != null)
                {
                    lst.add(new DockerLayerChunkCompactWithMetadata(m_chunkEntry.hash, m_chunkEntry.size, hasSubStructure()));
                }
            }

            void getChunks(List<DockerLayerChunkCompact> lst) throws
                                                              Exception
            {
                lst.add(m_chunkHeader);

                if (m_chunkEntry != null)
                {
                    if (hasSubStructure())
                    {
                        analyzeSubChunks();

                        if (m_subChunks != null)
                        {
                            lst.addAll(m_subChunks);
                        }
                        else
                        {
                            lst.add(m_chunkEntry);
                        }
                    }
                    else
                    {
                        lst.add(m_chunkEntry);
                    }
                }
            }

            //--//

            void finalizeDetails(ChunkLocation blob,
                                 long currentOffset) throws
                                                     IOException
            {
                offsetEntryEnd = currentOffset;

                m_chunkHeader = markChunk(blob, offsetHeaderStart, offsetHeaderEnd - offsetHeaderStart);

                long entrySize = offsetEntryEnd - offsetHeaderEnd;
                if (entrySize != 0)
                {
                    m_chunkEntry = markChunk(blob, offsetHeaderEnd, entrySize);
                    m_lookupSubChunks.put(m_chunkEntry.hash, this);
                }
            }

            //--//

            private boolean hasSubStructure()
            {
                return m_chunkEntry != null && entryName != null && entryName.endsWith(".jar");
            }

            private void analyzeSubChunks() throws
                                            Exception
            {
                if (m_subChunks == null && hasSubStructure())
                {
                    class SubChunkBuilder
                    {
                        private final List<DockerLayerChunkCompact> subChunks = Lists.newArrayList();
                        private       ChunkLocation                 blob;
                        private       long                          lastOffset;
                        private       ZipWalker.ArchiveEntry        lastJarEntry;

                        private SubChunkBuilder()
                        {
                            blob = findLayerChunk(m_chunkEntry.hash);
                        }

                        private void process() throws
                                               Exception
                        {
                            // Only get the part actually covered by the JAR file. There's a spillover due to TAR alignments.
                            ChunkLocation subBlob = blob.extract(0, sizeContent);

                            try (InputStream stream = subBlob.openForRead())
                            {
                                try
                                {
                                    //
                                    // If we detect a JAR, we want to analyze its entries.
                                    // That way we get a much better picture of the differences from build to build, greatly decreasing the download size.
                                    //
                                    // There's one little nuisance:
                                    //
                                    //    Zip files contain a Table of Contents at the end of the file.
                                    //    We don't get public access to it, so we can't diff it.
                                    //    We could recreate it, but at this point it's too dangerous, Docker requires bitwize identical layers or it will reject the image.
                                    //
                                    ZipWalker.walk(stream, (jarEntry) ->
                                    {
                                        if (lastJarEntry != null && lastJarEntry.isDirectory())
                                        {
                                            // Don't emit a marker for entries after directories, since directories are short.
                                        }
                                        else
                                        {
                                            addMarker(jarEntry.getFileOffset());
                                        }

                                        LoggerInstance.debugVerbose("JAR FRAGMENT: %s : %s : %08x : %s", id, entryName, jarEntry.getFileOffset(), jarEntry.getName());

                                        lastJarEntry = jarEntry;
                                        return true;
                                    });

                                    if (lastJarEntry != null)
                                    {
                                        //
                                        // Emit a marker at the location of the end of the last entry.
                                        // A Zip file ends with a table of contents, which doesn't get enumerated by ZipInputStream.
                                        //
                                        addMarker(lastJarEntry.getFileOffset() + lastJarEntry.getFileSize());
                                        LoggerInstance.debugVerbose("JAR FRAGMENT: %s : %s : %08x : END", id, entryName, lastJarEntry.getFileOffset() + lastJarEntry.getFileSize());
                                    }
                                }
                                catch (Throwable e)
                                {
                                    LoggerInstance.debugVerbose("Encountered a problem parsing JAR %s: %s", entryName, e);
                                }
                            }

                            //
                            // Emit a marker at the end of the blob.
                            //
                            addMarker(blob.getSize());
                        }

                        private void addMarker(long offset) throws
                                                            IOException
                        {
                            if (lastOffset < offset)
                            {
                                long size = offset - lastOffset;

                                DockerLayerChunkCompact chunk = markChunk(blob, lastOffset, size);
                                subChunks.add(chunk);

                                lastOffset = offset;
                            }
                        }
                    }

                    SubChunkBuilder subChunkBuilder = new SubChunkBuilder();

                    subChunkBuilder.process();

                    m_subChunks = subChunkBuilder.subChunks;
                }
            }
        }

        //--//

        public final String id;
        public       byte[] version;
        public       byte[] json;

        private ChunkLocation m_uncompressed;
        private long          m_uncompressedLength;

        private ChunkLocation m_compressed;
        private long          m_compressedLength;

        private       List<ChunkDetails>                     m_chunkDetails;
        private final Map<Encryption.Sha1Hash, ChunkDetails> m_lookupSubChunks = Maps.newHashMap();

        private WeakReference<List<DockerLayerChunkCompactWithMetadata>> m_weakChunksWithMetadata;
        private WeakReference<List<DockerLayerChunkCompact>>             m_weakChunks;

        //--//

        public Layer(String layerId)
        {
            id = layerId;
        }

        //--//

        public long getSize() throws
                              IOException
        {
            ensureUncompressed();

            return m_uncompressedLength;
        }

        public long getCompressedSize() throws
                                        IOException
        {
            ensureCompressed();

            return m_compressedLength;
        }

        private void setSize(long size,
                             boolean compressed)
        {
            if (compressed)
            {
                m_compressed = null;
                m_compressedLength = size;
            }
            else
            {
                m_uncompressed = null;
                m_uncompressedLength = size;
            }
        }

        //--//

        public List<DockerLayerChunkCompactWithMetadata> getChunksWithMetadata() throws
                                                                                 Exception
        {
            List<DockerLayerChunkCompactWithMetadata> lst = m_weakChunksWithMetadata != null ? m_weakChunksWithMetadata.get() : null;
            if (lst == null)
            {
                lst = Lists.newArrayList();

                for (ChunkDetails chunkDetail : analyzeChunks())
                {
                    chunkDetail.getChunksWithMetadata(lst);
                }

                m_weakChunksWithMetadata = new WeakReference<>(lst);
            }

            return lst;
        }

        public List<DockerLayerChunkCompact> getSubchunks(Encryption.Sha1Hash hash) throws
                                                                                    Exception
        {
            analyzeChunks();

            ChunkDetails chunk = m_lookupSubChunks.get(hash);
            if (chunk != null)
            {
                chunk.analyzeSubChunks();
                return chunk.m_subChunks;
            }

            return null;
        }

        public List<DockerLayerChunkCompact> getChunks() throws
                                                         Exception
        {
            List<DockerLayerChunkCompact> lst = m_weakChunks != null ? m_weakChunks.get() : null;
            if (lst == null)
            {
                lst = Lists.newArrayList();

                for (ChunkDetails chunkDetail : analyzeChunks())
                {
                    chunkDetail.getChunks(lst);
                }

                m_weakChunks = new WeakReference<>(lst);
            }

            return lst;
        }

        public ChunkLocation getBlobForRead(boolean compressed) throws
                                                                IOException
        {
            return compressed ? ensureCompressed() : ensureUncompressed();
        }

        public ChunkLocation getBlobForWrite(boolean compressed) throws
                                                                 IOException
        {
            if (compressed)
            {
                if (m_compressed == null)
                {
                    m_compressed = createChunk(m_compressedLength);
                }

                return m_compressed;
            }
            else
            {
                if (m_uncompressed == null)
                {
                    m_uncompressed = createChunk(m_uncompressedLength);
                }

                return m_uncompressed;
            }
        }

        public byte[] readChunk(long offset,
                                int size,
                                boolean compressed) throws
                                                    IOException
        {
            ChunkLocation blob = getBlobForRead(compressed);
            try (InputStream stream = blob.openForRead())
            {
                stream.skip(offset);

                return IOUtils.readFully(stream, size);
            }
        }

        public boolean hasContents()
        {
            return m_uncompressed != null;
        }

        public void setContents(InputStream input,
                                long size) throws
                                           Exception
        {
            m_uncompressed = createChunkFromStream(input, size);
            m_uncompressedLength = size;
        }

        private ChunkLocation ensureUncompressed() throws
                                                   IOException
        {
            if (m_uncompressed == null)
            {
                if (m_compressed == null)
                {
                    throw Exceptions.newRuntimeException("Internal error: no compressed blob for layer %s", id);
                }

                m_uncompressed = m_compressed.uncompress(m_uncompressedLength);
            }

            return m_uncompressed;
        }

        private ChunkLocation ensureCompressed() throws
                                                 IOException
        {
            if (m_compressed == null)
            {
                if (m_uncompressed == null)
                {
                    throw Exceptions.newRuntimeException("Internal error: no uncompressed blob for layer %s", id);
                }

                m_compressed = m_uncompressed.compress();
                m_compressedLength = m_compressed.getSize();
            }

            return m_compressed;
        }

        private void emit(TarBuilder builder) throws
                                              IOException
        {
            builder.addAsDir(null, id, 0755);
            builder.addAsBytes(id, c_blob_VERSION, version, 0644);
            builder.addAsBytes(id, c_blob_JSON, json, 0644);

            ChunkLocation blob = ensureUncompressed();

            try (InputStream stream = blob.openForRead())
            {
                builder.addAsStream(id, c_blob_TAR, stream, (int) blob.getSize(), 0644);
            }
        }

        //--//

        private List<ChunkDetails> analyzeChunks() throws
                                                   Exception
        {
            if (m_chunkDetails == null)
            {
                List<ChunkDetails> chunks = Lists.newArrayList();

                ChunkLocation blob = ensureUncompressed();
                try (InputStream stream = blob.openForRead())
                {
                    TarWalker.walk(stream, false, (entry) ->
                    {
                        ChunkDetails chunk = new ChunkDetails();

                        chunk.offsetHeaderStart = entry.getHeaderOffset();
                        chunk.offsetHeaderEnd = chunk.offsetHeaderStart + entry.getHeaderLength();
                        chunk.entryName = entry.name;
                        chunk.sizeContent = entry.size;

                        finalizeDetailsOfLastChunk(blob, chunks, chunk.offsetHeaderStart);

                        chunks.add(chunk);

                        return true;
                    });
                }

                finalizeDetailsOfLastChunk(blob, chunks, blob.getSize());

                m_chunkDetails = chunks;
            }

            return m_chunkDetails;
        }

        private void finalizeDetailsOfLastChunk(ChunkLocation blob,
                                                List<ChunkDetails> chunks,
                                                long currentOffset) throws
                                                                    IOException
        {
            // We don't know the extra size of a chunk until we move to the next Tar entry.
            // We need to do a fixup of the previous entry.
            ChunkDetails previousChunk = CollectionUtils.lastElement(chunks);
            if (previousChunk != null)
            {
                previousChunk.finalizeDetails(blob, currentOffset);
            }
        }

        private DockerLayerChunkCompact markChunk(ChunkLocation blob,
                                                  long offset,
                                                  long size) throws
                                                             IOException
        {
            ChunkLocation blobNew = blob.extract(offset, size);

            DockerLayerChunkCompact chunk;

            try (InputStream chunkStream = blobNew.openForRead())
            {
                Encryption.Sha1Hash hash = new Encryption.Sha1Hash(Encryption.computeSha1(chunkStream));
                chunk = new DockerLayerChunkCompact(hash, size);
            }

            registerLayerChunk(chunk.hash, blobNew);
            LoggerInstance.debugVerbose("TAR FRAGMENT: %s : %08x - %08x : %s", id, offset, offset + size, chunk.hash);

            return chunk;
        }
    }

    public class ChunkLocation
    {
        private final long m_offset;
        private final long m_length;

        private ChunkLocation(long offset,
                              long length)
        {
            m_offset = offset;
            m_length = length;
        }

        long getOffset()
        {
            return m_offset;
        }

        public long getSize()
        {
            return m_length;
        }

        public InputStream openForRead()
        {
            return m_state.heap.resource.sliceAsInputStream(m_offset, m_length);
        }

        public MemoryMappedHeap.SeekableInputOutputStream openForWrite()
        {
            return m_state.heap.resource.sliceAsOutputStream(m_offset, m_length);
        }

        public ChunkLocation extract(long offset,
                                     long length)
        {
            if (offset < 0 || length < 0)
            {
                throw new IndexOutOfBoundsException();
            }

            if (offset + length > m_length)
            {
                throw new IndexOutOfBoundsException();
            }

            return new ChunkLocation(m_offset + offset, length);
        }

        private ChunkLocation uncompress(long uncompressedSize) throws
                                                                IOException
        {
            try (InputStream input = openForRead())
            {
                try (GZIPInputStream inputUncompressed = new GZIPInputStream(input))
                {
                    return createChunkFromStream(inputUncompressed, uncompressedSize);
                }
            }
        }

        private ChunkLocation compress() throws
                                         IOException
        {
            try (InputStream input = openForRead())
            {
                try (MemoryMappedHeap.SkippableOutputStream output = m_state.heap.resource.allocateAsOutputStream())
                {
                    try (GZIPOutputStream outputCompressed = new GZIPOutputStream(output))
                    {
                        long bytesCopied = IOUtils.copyLarge(input, outputCompressed, 0, m_length);
                        if (bytesCopied != m_length)
                        {
                            throw Exceptions.newRuntimeException("Failed to compress contents: expected = %,d , actual = %,d", m_length, bytesCopied);
                        }
                    }

                    return new ChunkLocation(output.absolutePosition(), output.position());
                }
            }
        }
    }

    public class Reservation implements AutoCloseable
    {
        private final AsyncMutex.Holder m_holder;

        private Reservation(AsyncMutex.Holder holder)
        {
            m_holder = holder;
        }

        @Override
        public void close()
        {
            release();

            m_holder.close();
        }

        //--//

        public ImageAnalysisReport analyzeAllImages(String targetRepo) throws
                                                                       Exception
        {
            var                 report = new ImageAnalysisReport();
            List<ImageIdentity> images = Lists.newArrayList();

            List<ImageSummary> imageSummaries = callWithHelperAndAutoRetry(null, (helper) -> helper.listImages(false, null, true));

            for (ImageSummary is : imageSummaries)
            {
                if (is.repoTags != null)
                {
                    for (String tag : is.repoTags)
                    {
                        DockerImageIdentifier img = new DockerImageIdentifier(tag);
                        if (StringUtils.equals(img.getRegistryAddress(), targetRepo))
                        {
                            var id = new ImageIdentity();
                            id.sha = is.id;
                            id.tag = img;
                            images.add(id);
                            break;
                        }
                    }
                }
            }

            try
            {
                report.newPackages.add(analyzeInner(images));
            }
            catch (Throwable t)
            {
                report.failures.add(String.format("Failed to analyze all images at once, due to %s", t.getMessage()));

                for (var image : images)
                {
                    try
                    {
                        report.newPackages.add(analyzeInner(Lists.newArrayList(image)));
                    }
                    catch (Throwable t2)
                    {
                        report.failures.add(String.format("Failed to analyze image '%s', due to %s", image.tag, t2.getMessage()));
                    }
                }
            }

            return report;
        }

        //--//

        public PackageOfImages createPackage(byte[] manifests,
                                             byte[] repositories) throws
                                                                  IOException
        {
            return DockerImageDownloader.this.createPackage(manifests, repositories);
        }

        public PackageOfImages analyze(UserInfo user,
                                       DockerImageIdentifier img) throws
                                                                  Exception
        {
            return DockerImageDownloader.this.analyze(user, img);
        }

        //--//

        public Layer findLayer(String layerId)
        {
            return DockerImageDownloader.this.findLayer(layerId);
        }

        public Layer prepareNewLayer(String layerId,
                                     byte[] version,
                                     byte[] json,
                                     long tarSize)
        {
            Layer l = new Layer(layerId);

            l.version = version;
            l.json = json;
            l.setSize(tarSize, false);

            return l;
        }

        public Layer prepareNewLayer(String layerId,
                                     byte[] version,
                                     byte[] json,
                                     long tarSize,
                                     long tarCompressedSize)
        {
            Layer l = prepareNewLayer(layerId, version, json, tarSize);

            l.setSize(tarCompressedSize, true);

            return l;
        }

        public Layer registerLayer(Layer layer)
        {
            return DockerImageDownloader.this.registerLayer(layer);
        }

        //--//

        public ChunkLocation findLayerChunk(Encryption.Sha1Hash hash)
        {
            return DockerImageDownloader.this.findLayerChunk(hash);
        }

        public void registerLayerChunk(Encryption.Sha1Hash hash,
                                       ChunkLocation blob)
        {
            DockerImageDownloader.this.registerLayerChunk(hash, blob);
        }
    }

    //--//

    static class State
    {
        final ResourceAutoCleaner<MemoryMappedHeap>   heap           = new ResourceAutoCleaner<>(this, new MemoryMappedHeap("DockerImageDownloader", 64 * 1024 * 1024, 0));
        final Set<String>                             analyzedImages = Sets.newHashSet();
        final Map<String, PackageOfImages>            packages       = Maps.newHashMap();
        final Map<String, Layer>                      layers         = Maps.newHashMap();
        final Map<Encryption.Sha1Hash, ChunkLocation> chunkLookup    = Maps.newHashMap();

        void close()
        {
            heap.clean();
        }
    }

    private final AsyncMutex m_mutex = new AsyncMutex();
    private       State      m_state = new State();
    private final long       m_purgeDelayInSeconds;

    private int                m_outstandingReservations;
    private ScheduledFuture<?> m_task;

    public DockerImageDownloader(Duration purgeDelay)
    {
        m_purgeDelayInSeconds = purgeDelay.getSeconds();
    }

    //--//

    public void close() throws
                        Exception
    {
        synchronized (m_mutex)
        {
            m_state.close();
            m_state = new State();
        }
    }

    public CompletableFuture<Reservation> acquire() throws
                                                    Exception
    {
        synchronized (m_mutex)
        {
            if (m_task != null)
            {
                m_task.cancel(false);
                m_task = null;
            }

            m_outstandingReservations++;
        }

        AsyncMutex.Holder holder = await(m_mutex.acquire());

        return wrapAsync(new Reservation(holder));
    }

    private void release()
    {
        synchronized (m_mutex)
        {
            m_outstandingReservations--;

            if (m_outstandingReservations == 0 && m_state.heap.resource.length() > 0)
            {
                m_task = Executors.scheduleOnDefaultPool(this::purge, m_purgeDelayInSeconds, TimeUnit.SECONDS);
            }
        }
    }

    private CompletableFuture<Void> purge() throws
                                            Exception
    {
        m_task = null;

        try (Reservation reservation = await(acquire()))
        {
            LoggerInstance.info("Releasing state...");
            try
            {
                close();
            }
            catch (Exception e)
            {
                // Ignore failures.
            }
        }

        return wrapAsync(null);
    }

    //--//

    public static Set<String> enumerateDiffIDs() throws
                                                 Exception
    {
        Set<String> diffIDs = Sets.newHashSet();

        List<ImageSummary> imageSummaries = callWithHelperAndAutoRetry(null, (helper) -> helper.listImages(false, null, false));

        for (ImageSummary is : imageSummaries)
        {
            Image image = callWithHelperAndAutoRetry(null, (helper) -> helper.inspectImage(new DockerImageIdentifier(is.id)));

            if (image.rootFS != null && image.rootFS.layers != null)
            {
                diffIDs.addAll(image.rootFS.layers);
            }
        }

        return diffIDs;
    }

    //--//

    private Layer findLayer(String layerId)
    {
        return m_state.layers.get(layerId);
    }

    private PackageOfImages createPackage(byte[] manifests,
                                          byte[] repositories) throws
                                                               IOException
    {
        PackageOfImages pkg = new PackageOfImages();

        pkg.setManifests(manifests);
        pkg.setRepositories(repositories);

        return pkg;
    }

    private PackageOfImages analyze(UserInfo user,
                                    DockerImageIdentifier img) throws
                                                               Exception
    {
        PackageOfImages pkg = m_state.packages.get(img.getFullName());
        if (pkg == null)
        {
            LoggerInstance.info("Analyzing image '%s'...", img);

            Image imageDesc = callWithHelperAndAutoRetry(user, (helper) -> helper.inspectImageOrNull(img));
            if (imageDesc != null) // Existing image, don't pull.
            {
                pkg = analyzeInner(img);
            }
            else
            {
                try
                {
                    LoggerInstance.info("Pulling image '%s'...", img);
                    Image image = callWithHelperAndAutoRetry(user, (helper) ->
                    {
                        try
                        {
                            return helper.pullImage(img, null);
                        }
                        catch (NotFoundException e)
                        {
                            return null;
                        }
                    });

                    if (image == null)
                    {
                        throw new NotFoundException(String.format("Image '%s' not found", img));
                    }

                    LoggerInstance.info("Pulled image '%s'", img);
                    pkg = analyzeInner(img);
                }
                finally
                {
                    callWithHelperAndAutoRetry(null, (helper) -> helper.removeImage(img, false));
                }
            }

            LoggerInstance.info("Analyzed image '%s'", img);
            m_state.packages.put(img.fullName, pkg);
        }

        return pkg;
    }

    private PackageOfImages analyzeInner(DockerImageIdentifier img) throws
                                                                    Exception
    {
        var id = new ImageIdentity();
        id.tag = img;

        PackageOfImages pkg = analyzeInner(Lists.newArrayList(id));

        Image imgInspection = callWithHelperAndAutoRetry(null, (helper) -> helper.inspectImage(img));
        pkg.imageSha = imgInspection.id;

        return pkg;
    }

    private PackageOfImages analyzeInner(List<ImageIdentity> images) throws
                                                                     Exception
    {
        PackageOfImages pkg = new PackageOfImages();

        Map<String, Layer> newLayers = Maps.newHashMap();

        List<DockerImageIdentifier> newTags = CollectionUtils.transformToListNoNulls(images, (img) -> img.sha == null || !m_state.analyzedImages.contains(img.sha) ? img.tag : null);
        if (!newTags.isEmpty())
        {
            callWithHelperAndAutoRetry(null, (helper) ->
            {
                helper.setTimeout(20 * 60 * 1000);

                TarWalker.walk(helper.exportImages(newTags), false, (entry) ->
                {
                    if (!entry.isDirectory())
                    {
                        String[] parts = StringUtils.split(entry.name, '/');

                        if (parts.length == 2)
                        {
                            String layerId = parts[0];

                            Layer l = findLayer(layerId);
                            if (l == null)
                            {
                                l = newLayers.get(layerId);
                                if (l == null)
                                {
                                    l = new Layer(layerId);
                                    newLayers.put(layerId, l);
                                }
                            }

                            pkg.linkLayer(l);

                            switch (parts[1])
                            {
                                case c_blob_VERSION:
                                    if (l.version == null)
                                    {
                                        l.version = IOUtils.readFully(entry.getStream(), (int) entry.size);
                                    }
                                    break;

                                case c_blob_JSON:
                                    if (l.json == null)
                                    {
                                        l.json = IOUtils.readFully(entry.getStream(), (int) entry.size);
                                    }
                                    break;

                                case c_blob_TAR:
                                    if (!l.hasContents())
                                    {
                                        l.setContents(entry.getStream(), entry.size);
                                    }
                                    break;
                            }
                        }
                        else if (parts.length == 1)
                        {
                            String fileName = parts[0];

                            switch (fileName)
                            {
                                case c_manifest:
                                {
                                    pkg.setManifests(IOUtils.readFully(entry.getStream(), (int) entry.size));
                                }
                                break;

                                case c_repositories:
                                {
                                    pkg.setRepositories(IOUtils.readFully(entry.getStream(), (int) entry.size));
                                }
                                break;

                                default:
                                    if (fileName.endsWith(".json"))
                                    {
                                        pkg.addImageDetails(fileName, IOUtils.readFully(entry.getStream(), (int) entry.size));
                                    }
                                    break;
                            }
                        }
                    }

                    return true;
                });

                return null;
            });

            for (Layer layer : newLayers.values())
            {
                registerLayer(layer);
            }

            //
            // Resolve diffId to layer.
            //
            for (ManifestItem manifestItem : pkg.manifests)
            {
                ImageDetails config = pkg.images.get(manifestItem.Config);

                for (int i = 0; i < manifestItem.Layers.length; i++)
                {
                    String   layerPath = manifestItem.Layers[i];
                    String[] parts     = StringUtils.split(layerPath, '/');

                    if (parts.length != 2 || !StringUtils.equals(parts[1], c_blob_TAR))
                    {
                        throw Exceptions.newRuntimeException("Unexpected format for layer path: %s", layerPath);
                    }

                    String layerId = parts[0];
                    Layer  layer   = pkg.layers.get(layerId);
                    if (layer == null)
                    {
                        throw Exceptions.newRuntimeException("Unknown layer: %s", layerId);
                    }

                    config.diffIdToLayer.put(config.contents.rootFS.diff_ids.get(i), layer);
                }
            }

            for (ImageIdentity image : images)
            {
                if (image.sha != null)
                {
                    m_state.analyzedImages.add(image.sha);
                }
            }
        }

        return pkg;
    }

    private Layer registerLayer(Layer layer)
    {
        return m_state.layers.putIfAbsent(layer.id, layer);
    }

    //--//

    private ChunkLocation createChunkFromStream(InputStream input,
                                                long size) throws
                                                           IOException
    {
        try (MemoryMappedHeap.SeekableInputOutputStream output = m_state.heap.resource.allocateAsFixedSizeOutputStream(size))
        {
            IOUtils.copyLarge(input, output);

            return new ChunkLocation(output.absolutePosition(), size);
        }
    }

    private ChunkLocation createChunk(long size) throws
                                                 IOException
    {
        try (MemoryMappedHeap.SeekableInputOutputStream output = m_state.heap.resource.allocateAsFixedSizeOutputStream(size))
        {
            return new ChunkLocation(output.absolutePosition(), size);
        }
    }

    private ChunkLocation findLayerChunk(Encryption.Sha1Hash hash)
    {
        return m_state.chunkLookup.get(hash);
    }

    private void registerLayerChunk(Encryption.Sha1Hash hash,
                                    ChunkLocation blob)
    {
        m_state.chunkLookup.putIfAbsent(hash, blob);
    }

    private static <T> T callWithHelperAndAutoRetry(UserInfo registryUser,
                                                    FunctionWithException<DockerHelper, T> callback) throws
                                                                                                     Exception
    {
        int maxRetries = 10;

        while (true)
        {
            try (DockerHelper helper = new DockerHelper(registryUser))
            {
                return callback.apply(helper);
            }
            catch (Throwable e)
            {
                if (maxRetries-- > 0)
                {
                    continue;
                }

                LoggerInstance.error("Operation failed with %s", e);

                throw e;
            }
        }
    }
}