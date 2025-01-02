package de.fernunihagen.dbis.anguillasearch.index;

import java.util.List;
import java.util.Properties;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Collections;

import de.fernunihagen.dbis.anguillasearch.helpers.AVLTree;
import de.fernunihagen.dbis.anguillasearch.helpers.Site;
import static de.fernunihagen.dbis.anguillasearch.index.IndexConfig.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * This class is now deprecated and has been replaced by the VectorIndex class.
 * 
 * The class represents a reverse index which maps a set of Sites to an index of
 * Tokens for use with a pagerank algorithm. The tokens are free of stopwords,
 * lemmatized and contain their TFIDF score for each of the Sites parsed.
 * 
 * @author Nico Beyer
 */
public class ReverseIndex {

    private List<String> specialCharacters;
    private List<String> stopwords;
    private StanfordCoreNLP pipeline;

    /**
     * An object of the SearchToken class saves all relevant information of a single
     * token for the searchQuery() method.
     */
    private class SearchToken implements Comparable<SearchToken> {

        private String documentId;
        private Double searchScore;

        /**
         * Create a new SearchToken object for the document with documentId.
         * 
         * @param documentId The id of the document the token represents.
         */
        public SearchToken(String documentId) {
            searchScore = 0.0d;
            this.documentId = documentId;
        }

        /**
         * Increment the searchScore by tfIdfScore.
         * 
         * @param tfIdfScore The value to increment the SearchScore by.
         */
        public void updateSearchScore(double tfIdfScore) {
            searchScore += tfIdfScore;
        }

        /**
         * Define a comparison function between tokens to establish a total order.
         */
        @Override
        public int compareTo(SearchToken other) {
            return this.searchScore.compareTo(other.searchScore);
        }

        /**
         * Just added for compliance with the Comparable contract.
         */
        @Override
        public boolean equals(Object other) {
            return this.searchScore.equals(other);
        }

        /**
         * Just added for compliance with the Comparable contract.
         */
        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    private AVLTree<Token> index = new AVLTree<>();

    /**
     * Create a new empty reverse index.
     * If this option is used use the parseSiteList() method to initialize the
     * index.
     */
    public ReverseIndex() {
        init();
    }

    /**
     * Create a new reverse index from the given List of Sites.
     * 
     * @param siteList The List of Sites to initialize the index.
     */
    public ReverseIndex(List<Site> siteList) {
        init();
        parseSiteList(siteList);
    }

    /**
     * Initialize basic lists and settings for the tokenization and lemmatization.
     */
    private void init() {
        this.specialCharacters = REVERSEINDEX_SPECIAL_CHARACTERS;
        this.stopwords = REVERSEINDEX_STOPWORDS;
        Properties props = new Properties();
        props.setProperty("annotators", REVERSEINDEX_PIPELINE_ANNOTATORS);
        pipeline = new StanfordCoreNLP(props);
    }

    /**
     * Parse a List of sites to create a new index.
     * A previously created index will be deleted upon calling this method!
     * 
     * @param siteList The List of Sites to be parsed.
     */
    public void parseSiteList(List<Site> siteList) {
        index = new AVLTree<>();

        TreeMap<String, DocInfo> documents = new TreeMap<>();

        for (Site site : siteList) {
            DocInfo docInfo = new DocInfo();

            // Convert site content into one lowercase string.
            StringBuilder siteContent = new StringBuilder(site.title.toLowerCase());

            for (String heading : site.headings)
                siteContent.append(heading.toLowerCase());

            siteContent.append(site.paragraphs.toLowerCase());

            // Tokenize and lemmatize the sites.
            CoreDocument content = new CoreDocument(siteContent.toString());
            pipeline.annotate(content);

            for (CoreLabel tok : content.tokens()) {
                String lemma = tok.lemma();
                if (specialCharacters.contains(lemma))
                    continue;
                docInfo.incrementTotalWordcount();
                // Remove stopwords.
                if (stopwords.contains(lemma))
                    continue;
                // Count how much each word has been used.
                docInfo.incrementWordCountOf(lemma);
                // Save processed tokens.
                addToken(lemma, site.url);
            }
            documents.put(site.url, docInfo);
        }

        // Calculate the TFIDF scores for each token.
        for (Token tok : index.getValuesInorder()) {
            String word = tok.getToken();
            double documentCount = documents.size();
            double docPerTerm = tok.getNrOfDocuments();

            for (String docId : tok.getDocuments()) {
                DocInfo docInfo = documents.get(docId);
                double tf = docInfo.getTfOf(word);
                double idf = Math.log(documentCount / docPerTerm);
                tok.setTfIdfScore(docId, tf * idf);
            }
        }
    }

    /**
     * Find the Token object of the given tokenWord in the index.
     * 
     * @param tokenWord The token word to be found.
     * @return The Token object that belongs to tokenWord or null if the token is
     *         not contained in the index.
     */
    public Token findToken(String tokenWord) {
        return index.retrieve(new Token(tokenWord));
    }

    /**
     * Check if the tokenWord is contained in the index.
     * 
     * @param tokenWord The token word to check for.
     * @return true if tokenWord is part of the index, false otherwise.
     */
    public boolean containsToken(String tokenWord) {
        return this.index.contains(new Token(tokenWord));
    }

    /**
     * Search for documents relevant to the search query.
     * 
     * @param query            The search query to be used.
     * @param maxNrOfDocuments The maximum number of results to be returned.
     * @return A List of String[] with String[0]= documentId, String[1]= document
     *         TFIDF-searchscore.
     */
    public List<String[]> searchQuery(String query, int maxNrOfDocuments) {
        LinkedList<String[]> formatedDocList = new LinkedList<>();

        // Tokenize and lemmatize the query.
        CoreDocument content = new CoreDocument(query.toLowerCase());
        pipeline.annotate(content);

        TreeMap<String, SearchToken> searchTree = new TreeMap<>();

        for (CoreLabel tok : content.tokens()) {
            String lemma = tok.lemma();

            // Remove stopwords and special characters.
            if (specialCharacters.contains(lemma) || stopwords.contains(lemma))
                continue;

            // Find out which documents belong to the token.
            Token foundToken = index.retrieve(new Token(lemma));
            if (foundToken == null)
                continue;

            for (String docId : foundToken.getDocuments()) {
                // For each document add up the TFIDF(token) to a sum per document.
                SearchToken searchToken = searchTree.getOrDefault(docId, new SearchToken(docId));
                searchToken.updateSearchScore(foundToken.getTfIdfScore(docId));
                searchTree.put(docId, searchToken);
            }
        }

        // Get a list sorted by TF-IDF score of the documents.
        List<SearchToken> sortedDocList = new LinkedList<>(searchTree.values());
        Collections.sort(sortedDocList, Collections.reverseOrder());

        // format a string from the top x documents and return.
        int i = 1;
        for (SearchToken token : sortedDocList) {
            formatedDocList.addLast(new String[] { token.documentId, token.searchScore.toString() });

            i++;
            if ((maxNrOfDocuments != -1) && (i > maxNrOfDocuments))
                break;
        }
        return formatedDocList;
    }

    /**
     * Search for documents relevant to the search query.
     * 
     * @param query The search query to be used.
     * @return A List of String[] with String[0]= documentId, String[1]= document
     *         TFIDF-searchscore.
     */
    public List<String[]> searchQuery(String query) {
        return searchQuery(query, -1);
    }

    /**
     * Add a document to the Token object that belongs to the tokenWord.
     * If the Token is not part of the index yet a Token will be created and added.
     * 
     * @param tokenWord The word that the Token object represents.
     * @param document  The document id to be added to the Token.
     */
    private void addToken(String tokenWord, String document) {
        Token foundToken = findToken(tokenWord);
        if (foundToken == null) {
            Token newToken = new Token(tokenWord);
            newToken.addDocument(document);
            index.insert(newToken);
        } else
            foundToken.addDocument(document);
    }
}
