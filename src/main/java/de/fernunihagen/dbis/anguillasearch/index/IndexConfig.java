package de.fernunihagen.dbis.anguillasearch.index;

import java.util.Arrays;
import java.util.List;

/**
 * The configuration values for the ReverseIndex and VectorIndex classes.
 */
public final class IndexConfig {
        private IndexConfig() {
        }

        // Stopwordlist to be filtered out after lemmatization.
        static final List<String> REVERSEINDEX_STOPWORDS = Arrays.asList("'s", "in", "further", "myself", "than",
                        "below",
                        "to", "should", "yours", "for", "have", "before", "my", "nor", "not", "s", "now", "their", "no",
                        "against", "under", "if", "does", "during", "herself", "him", "same", "been", "other", "will",
                        "who",
                        "these", "themselves", "are", "how", "while", "is", "himself", "some", "an", "then", "a", "had",
                        "can",
                        "each", "me", "your", "his", "being", "above", "but", "it", "between", "by", "do", "its", "too",
                        "only", "did", "up", "be", "this", "through", "down", "there", "her", "them", "so", "our", "he",
                        "about",
                        "they", "hers", "itself", "again", "as", "were", "when", "own", "until", "very", "theirs",
                        "whom",
                        "you", "any", "once", "because", "few", "or", "more", "don", "here", "t", "what", "with",
                        "into",
                        "from", "after", "has", "am", "ourselves", "out", "having", "at", "that", "all", "yourselves",
                        "just",
                        "over", "the", "such", "those", "yourself", "which", "where", "doing", "and", "of", "ours",
                        "was",
                        "most", "i", "she", "we", "why", "off", "on", "both");
        // Special characters to be filtered out after lemmatization.
        static final List<String> REVERSEINDEX_SPECIAL_CHARACTERS = Arrays.asList(":", ",", ".", "!", "|", "&", "'",
                        "[",
                        "]", "?", "-", "–", "_", "/", "\\", "{", "}", "@", "^", "(", ")", "<", ">", "\"", "%", "©",
                        "—", "$");

        static final String REVERSEINDEX_PIPELINE_ANNOTATORS = "tokenize, ssplit, pos, lemma";

}
