/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.collection.Memoizer;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class TermFrequencyInverseDocumentFrequencyVectorizer
{
    public class Document
    {
        private final String                     m_text;
        private final Map<String, AtomicInteger> m_termCounts;

        private final Set<String> m_nGrams;
        private       int         m_numTerms;

        Document(Memoizer memoizer,
                 String text)
        {
            m_termCounts = Maps.newHashMap();
            m_nGrams     = Sets.newHashSet();
            m_text       = text;

            List<String> ngrams = computeNgrams(memoizer, m_text, m_minNgram, m_maxNgram);
            for (String term : ngrams)
            {
                addTerm(term);
            }
        }

        public String getText()
        {
            return m_text;
        }

        public Set<String> getNgrams()
        {
            return m_nGrams;
        }

        private void addTerm(String term)
        {
            m_numTerms++;
            m_nGrams.add(term);

            AtomicInteger value = m_termCounts.get(term);
            if (value != null)
            {
                value.incrementAndGet();
            }
            else
            {
                m_termCounts.put(term, new AtomicInteger(1));
            }
        }

        private double[] computeTermFrequencies(String[] vocab)
        {
            double[] termFrequencies = new double[vocab.length];
            int      idx             = 0;

            for (String ngram : vocab)
            {
                AtomicInteger value = m_termCounts.get(ngram);
                if (value != null)
                {
                    termFrequencies[idx] = 1.0 * value.get() / m_numTerms;
                }
                idx++;
            }

            return termFrequencies;
        }
    }

    public class FittedVectorizer
    {
        private final double[]   m_idf;
        private final String[]   m_terms;
        private final double[][] m_documentMatrix;

        FittedVectorizer(List<String> documents,
                         int minDocFrequency)
        {
            Map<String, AtomicInteger> documentFrequency = Maps.newHashMap();

            for (String raw : documents)
            {
                Document doc = getDocument(raw);

                for (String ngram : doc.m_termCounts.keySet())
                {
                    AtomicInteger value = documentFrequency.get(ngram);
                    if (value != null)
                    {
                        value.incrementAndGet();
                    }
                    else
                    {
                        documentFrequency.put(ngram, new AtomicInteger(1));
                    }
                }
            }

            List<String> removals = CollectionUtils.transformToListNoNulls(documentFrequency.entrySet(), (entry) ->
            {
                AtomicInteger value = entry.getValue();
                return value.get() < minDocFrequency ? entry.getKey() : null;
            });

            for (String key : removals)
            {
                documentFrequency.remove(key);
            }

            m_idf   = new double[documentFrequency.size()];
            m_terms = new String[documentFrequency.size()];

            double totalDocs = documents.size() + 1;
            int    idx       = 0;
            for (Map.Entry<String, AtomicInteger> entry : documentFrequency.entrySet())
            {
                AtomicInteger value = entry.getValue();

                int numDocs = value.get() + 1;
                m_idf[idx]   = 1 + Math.log(totalDocs / numDocs);
                m_terms[idx] = entry.getKey();
                idx++;
            }

            m_documentMatrix = new double[documents.size()][];
            for (int i = 0; i < documents.size(); i++)
            {
                m_documentMatrix[i] = transformSingle(documents.get(i));
            }
        }

        private double[] transformSingle(String document)
        {
            Document doc   = getDocument(document);
            double[] tf    = doc.computeTermFrequencies(m_terms);
            double[] tfidf = new double[m_terms.length];

            double sum = 0;
            for (int idx = 0; idx < tf.length; idx++)
            {
                tfidf[idx] = tf[idx] * m_idf[idx];
                sum += Math.pow(tfidf[idx], 2);
            }

            if (sum > 0)
            {
                sum = Math.sqrt(sum);
                for (int idx = 0; idx < tfidf.length; idx++)
                {
                    tfidf[idx] = tfidf[idx] / sum;
                }
            }

            return tfidf;
        }

        public String[] getTerms()
        {
            return m_terms;
        }
    }

    static class DocumentSet
    {
        private final List<String> m_docs;
        private final int          m_minDocFrequency;

        DocumentSet(List<String> docs,
                    int minDocFrequency)
        {
            m_docs            = docs;
            m_minDocFrequency = minDocFrequency;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            DocumentSet that = Reflection.as(o, DocumentSet.class);
            if (that == null)
            {
                return false;
            }

            return m_minDocFrequency == that.m_minDocFrequency && Objects.equals(m_docs, that.m_docs);
        }

        @Override
        public int hashCode()
        {
            int result = 1;

            result = 31 * result + Integer.hashCode(m_minDocFrequency);
            result = 31 * result + m_docs.hashCode();

            return result;
        }
    }

    public static class Result
    {
        public double[][] documentMatrix;
        public double[]   textResult;
        public double[]   scores;
        public String[]   terms;
    }

    private final Memoizer                           m_memoizer;
    private final Map<String, Document>              m_documents;
    private final Map<DocumentSet, FittedVectorizer> m_fittedVectorizers;

    private final int m_minNgram;
    private final int m_maxNgram;

    public TermFrequencyInverseDocumentFrequencyVectorizer(Memoizer memoizer,
                                                           int minNgram,
                                                           int maxNgram)
    {
        m_memoizer          = memoizer;
        m_documents         = Maps.newHashMap();
        m_fittedVectorizers = Maps.newHashMap();

        m_minNgram = Math.max(1, minNgram);
        m_maxNgram = Math.max(m_minNgram, maxNgram);
    }

    public Result score(List<String> docs,
                        String text,
                        int minDocFrequency)
    {
        Result result = new Result();

        FittedVectorizer vectorizer = m_fittedVectorizers.computeIfAbsent(new DocumentSet(docs, minDocFrequency), (key) -> new FittedVectorizer(docs, minDocFrequency));

        result.documentMatrix = vectorizer.m_documentMatrix;
        result.textResult     = vectorizer.transformSingle(text);
        result.scores         = new double[docs.size()];
        result.terms          = vectorizer.getTerms();

        for (int i = 0; i < result.documentMatrix.length; i++)
        {
            double score = cosineSimilarity(result.textResult, result.documentMatrix[i]);
            result.scores[i] = score;
        }

        return result;
    }

    public Document getDocument(String text)
    {
        Document doc = m_documents.get(text);
        if (doc != null)
        {
            return doc;
        }

        String textLowercase = m_memoizer.intern(text.toLowerCase());
        doc = m_documents.get(text);
        if (doc != null)
        {
            m_documents.put(text, doc);
            return doc;
        }

        doc = new Document(m_memoizer, textLowercase);
        m_documents.put(text, doc);
        m_documents.put(textLowercase, doc);
        return doc;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        TermFrequencyInverseDocumentFrequencyVectorizer that = Reflection.as(o, TermFrequencyInverseDocumentFrequencyVectorizer.class);
        if (that == null)
        {
            return false;
        }

        return m_minNgram == that.m_minNgram && m_maxNgram == that.m_maxNgram;
    }

    @Override
    public int hashCode()
    {
        int result = 1;

        result = 31 * result + Integer.hashCode(m_minNgram);
        result = 31 * result + Integer.hashCode(m_maxNgram);

        return result;
    }

    public static List<String> computeNgrams(Memoizer memoizer,
                                             String text,
                                             int minNgram,
                                             int maxNgram)
    {
        List<String> words = NormalizationEngine.splitAndLowercase(text, " ,;:+*()[]{}\"'=-/\\.0123456789");
        maxNgram = Math.min(maxNgram, words.size());
        List<String> ngrams = Lists.newArrayList();

        for (int i = minNgram; i <= maxNgram; i++)
        {
            for (int j = 0; j <= words.size() - i; j++)
            {
                String term = StringUtils.join(words.subList(j, i + j), " ");
                ngrams.add(memoizer.intern(term));
            }
        }

        return ngrams;
    }

    private static double cosineSimilarity(double[] a,
                                           double[] b)
    {
        double result = 0;
        for (int i = 0; i < a.length; i++)
        {
            result += a[i] * b[i];
        }
        return result;
    }
}
