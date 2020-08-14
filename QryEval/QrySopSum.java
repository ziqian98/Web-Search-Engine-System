import java.io.*;

/**
 *  The SUM operator for BM25 retrieval models.
 */

public class QrySopSum extends QrySop{

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        return this.docIteratorHasMatchMin (r);
    }

    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelBM25) {
            return this.getScoreBM25(r);
        }else{
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the SUM operator.");
        }
    }

    public double getScoreBM25(RetrievalModel r) throws IOException{
        double score = 0;
        if(this.docIteratorHasMatchCache()){
            for(int i=0; i<this.args.size(); i++){
                if(this.args.get(i).docIteratorHasMatch(r) &&
                        this.args.get(i).docIteratorGetMatch() == this.docIteratorGetMatch()){
                    score = score + ((QrySop)this.args.get(i)).getScore(r);
                }
            }
            return score;
        }else {
            return score;
        }
    }

    /**
     *  getDefaultScore for the BM25 retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    // no default score in BM25
    public double getDefaultScore(RetrievalModel r, long docid)throws IOException {
        return 0.0;
    }
}
