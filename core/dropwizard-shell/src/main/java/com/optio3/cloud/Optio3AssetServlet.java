/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.optio3.util.TimeUtils;
import io.dropwizard.servlets.assets.ByteRange;
import io.dropwizard.servlets.assets.ResourceURL;
import org.apache.commons.lang3.StringUtils;

/**
 * This is a copy of Dropwizard's AssetServlet, to fix a caching issue (the cache logic should be AND'ing the conditions, instead of OR'ing them).
 */
public class Optio3AssetServlet extends HttpServlet
{
    public static boolean allowIndexCaching = false;

    private static final long        serialVersionUID = 6393345594784987908L;
    private static final CharMatcher SLASHES          = CharMatcher.is('/');

    private class CachedAsset
    {
        final URL     resourceUrl;
        final boolean isIndex;

        String                eTag;
        WeakReference<byte[]> resource;

        private CachedAsset(URL resourceUrl,
                            boolean isIndex)
        {
            this.resourceUrl = resourceUrl;
            this.isIndex     = isIndex;
        }

        private byte[] getResource() throws
                                     IOException
        {
            byte[] res = null;

            if (resource != null)
            {
                res = resource.get();
            }

            if (res == null)
            {
                res = readResource(resourceUrl);
            }

            if (res != null)
            {
                resource = new WeakReference<>(res);
            }

            return res;
        }

        private String getETag() throws
                                 IOException
        {
            if (this.eTag == null)
            {
                byte[] buf = getResource();
                this.eTag = '"' + Hashing.murmur3_128()
                                         .hashBytes(buf)
                                         .toString() + '"';
            }

            return this.eTag;
        }

        private boolean isCachedClientSide(HttpServletRequest req) throws
                                                                   IOException
        {
            if (isIndex)
            {
                if (!allowIndexCaching)
                {
                    return false;
                }

                if (req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE) < m_bootTime)
                {
                    return false;
                }
            }
            else
            {
                if (!StringUtils.equals(getETag(), req.getHeader(HttpHeaders.IF_NONE_MATCH)))
                {
                    return false;
                }
            }

            return true;
        }

        private void addHeaders(HttpServletResponse resp) throws
                                                          IOException
        {
            if (isIndex)
            {
                if (allowIndexCaching)
                {
                    resp.setDateHeader(HttpHeaders.LAST_MODIFIED, m_bootTime);
                }
                else
                {
                    resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
                }
            }
            else
            {
                resp.setHeader(HttpHeaders.ETAG, getETag());
            }
        }
    }

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;

    private final String                    m_resourcePath;
    private final String                    m_uriPath;
    private final String                    m_indexFile;
    private final Charset                   m_defaultCharset;
    private final Function<String, Boolean> m_allowAssetAccess;
    private final long                      m_bootTime;
    private final Map<URL, CachedAsset>     m_assets = Maps.newHashMap();

    /**
     * Creates a new {@code Optio3AssetServlet} that serves static assets loaded from {@code resourceURL}
     * (typically a file: or jar: URL). The assets are served at URIs rooted at {@code m_uriPath}. For
     * example, given a {@code resourceURL} of {@code "file:/data/assets"} and a {@code m_uriPath} of
     * {@code "/js"}, an {@code Optio3AssetServlet} would serve the contents of {@code
     * /data/assets/example.js} in response to a request for {@code /js/example.js}. If a directory
     * is requested and {@code m_indexFile} is defined, then {@code Optio3AssetServlet} will attempt to
     * serve a file with that name in that directory. If a directory is requested and {@code
     * m_indexFile} is null, it will serve a 404.
     *
     * @param resourcePath   the base URL from which assets are loaded
     * @param uriPath        the URI path fragment in which all requests are rooted
     * @param indexFile      the filename to use when directories are requested, or null to serve no
     *                       indexes
     * @param defaultCharset the default character set
     */
    public Optio3AssetServlet(String resourcePath,
                              String uriPath,
                              String indexFile,
                              Charset defaultCharset,
                              Function<String, Boolean> allowAssetAccess)
    {
        final String trimmedPath = SLASHES.trimFrom(resourcePath);
        m_resourcePath = trimmedPath.isEmpty() ? trimmedPath : trimmedPath + '/';

        final String trimmedUri = SLASHES.trimTrailingFrom(uriPath);
        m_uriPath          = trimmedUri.isEmpty() ? "/" : trimmedUri;
        m_indexFile        = indexFile;
        m_defaultCharset   = defaultCharset;
        m_allowAssetAccess = allowAssetAccess;

        m_bootTime = TimeUtils.nowEpochSeconds() * 1000;
    }

    public URL getResourceURL()
    {
        return Resources.getResource(m_resourcePath);
    }

    public String getUriPath()
    {
        return m_uriPath;
    }

    public String getIndexFile()
    {
        return m_indexFile;
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws
                                                   IOException
    {
        try
        {
            final StringBuilder builder = new StringBuilder(req.getServletPath());
            if (req.getPathInfo() != null)
            {
                builder.append(req.getPathInfo());
            }

            final CachedAsset cachedAsset = loadAsset(builder.toString());
            if (cachedAsset == null)
            {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (cachedAsset.isCachedClientSide(req))
            {
                resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            final String rangeHeader = req.getHeader(HttpHeaders.RANGE);

            final byte[]             resource       = cachedAsset.getResource();
            final int                resourceLength = resource.length;
            ImmutableList<ByteRange> ranges         = ImmutableList.of();

            boolean usingRanges = false;
            // Support for HTTP Byte Ranges
            // http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
            if (rangeHeader != null)
            {

                final String ifRange = req.getHeader(HttpHeaders.IF_RANGE);

                if (ifRange == null || cachedAsset.eTag.equals(ifRange))
                {
                    try
                    {
                        ranges = parseRangeHeader(rangeHeader, resourceLength);
                    }
                    catch (NumberFormatException e)
                    {
                        resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    if (ranges.isEmpty())
                    {
                        resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                    usingRanges = true;

                    resp.addHeader(HttpHeaders.CONTENT_RANGE,
                                   "bytes " + Joiner.on(",")
                                                    .join(ranges) + "/" + resourceLength);
                }
            }

            cachedAsset.addHeaders(resp);

            final String mimeTypeOfExtension = req.getServletContext()
                                                  .getMimeType(req.getRequestURI());
            MediaType mediaType = DEFAULT_MEDIA_TYPE;

            if (mimeTypeOfExtension != null)
            {
                try
                {
                    mediaType = MediaType.parse(mimeTypeOfExtension);
                    if (m_defaultCharset != null && mediaType.is(MediaType.ANY_TEXT_TYPE))
                    {
                        mediaType = mediaType.withCharset(m_defaultCharset);
                    }
                }
                catch (IllegalArgumentException ignore)
                {
                    // ignore
                }
            }

            if (mediaType.is(MediaType.ANY_VIDEO_TYPE) || mediaType.is(MediaType.ANY_AUDIO_TYPE) || usingRanges)
            {
                resp.addHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            }

            resp.setContentType(mediaType.type() + '/' + mediaType.subtype());

            if (mediaType.charset()
                         .isPresent())
            {
                resp.setCharacterEncoding(mediaType.charset()
                                                   .get()
                                                   .toString());
            }

            try (ServletOutputStream output = resp.getOutputStream())
            {
                if (usingRanges)
                {
                    for (ByteRange range : ranges)
                    {
                        output.write(resource, range.getStart(), range.getEnd() - range.getStart() + 1);
                    }
                }
                else
                {
                    output.write(resource);
                }
            }
        }
        catch (RuntimeException | URISyntaxException ignored)
        {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private CachedAsset loadAsset(String key) throws
                                              URISyntaxException
    {
        checkArgument(key.startsWith(m_uriPath));

        final String requestedResourcePath = SLASHES.trimFrom(key.substring(m_uriPath.length()));
        if (m_allowAssetAccess != null && m_allowAssetAccess.apply(requestedResourcePath) != Boolean.TRUE)
        {
            return null;
        }

        final String absoluteRequestedResourcePath = SLASHES.trimFrom(this.m_resourcePath + requestedResourcePath);
        boolean      isIndex                       = false;

        URL requestedResourceURL = getResourceUrl(absoluteRequestedResourcePath);
        if (ResourceURL.isDirectory(requestedResourceURL))
        {
            if (m_indexFile != null)
            {
                requestedResourceURL = getResourceUrl(absoluteRequestedResourcePath + '/' + m_indexFile);
                isIndex              = true;
            }
            else
            {
                // directory requested but no index file defined
                return null;
            }
        }

        CachedAsset asset = m_assets.get(requestedResourceURL);
        if (asset == null)
        {
            asset = new CachedAsset(requestedResourceURL, isIndex);
            m_assets.put(requestedResourceURL, asset);
        }

        return asset;
    }

    protected URL getResourceUrl(String absoluteRequestedResourcePath)
    {
        return Resources.getResource(absoluteRequestedResourcePath);
    }

    protected byte[] readResource(URL requestedResourceURL) throws
                                                            IOException
    {
        return Resources.toByteArray(requestedResourceURL);
    }

    /**
     * Parses a given Range header for one or more byte ranges.
     *
     * @param rangeHeader    Range header to parse
     * @param resourceLength Length of the resource in bytes
     *
     * @return List of parsed ranges
     */
    private ImmutableList<ByteRange> parseRangeHeader(final String rangeHeader,
                                                      final int resourceLength)
    {
        final ImmutableList.Builder<ByteRange> builder = ImmutableList.builder();
        if (rangeHeader.contains("="))
        {
            final String[] parts = rangeHeader.split("=");
            if (parts.length > 1)
            {
                final List<String> ranges = Splitter.on(",")
                                                    .trimResults()
                                                    .splitToList(parts[1]);
                for (final String range : ranges)
                {
                    builder.add(ByteRange.parse(range, resourceLength));
                }
            }
        }
        return builder.build();
    }
}
