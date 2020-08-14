

import java.io.*;

/**
 *  The WSUM operator for INDRI models.
 */

public class QrySopWsum extends QrySopWeight {

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        return this.docIteratorHasMatchMin (r);  //Only in Indri
    }


    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri(r);
        }else{
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WSUM operator.");
        }
    }


    /**
     *  getScore for the Indri retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */

    public double getScoreIndri(RetrievalModel r)throws  IOException{
        double score = 0.0;
        double finalScore = 0.0;
        for(int i=0; i<this.args.size();i++){

            if(this.args.get(i).docIteratorHasMatchCache()&&
                    this.docIteratorGetMatch()==this.args.get(i).docIteratorGetMatch()){
                score = ((QrySop)this.args.get(i)).getScore(r);

            }else {
                score = ((QrySop)this.args.get(i)).getDefaultScore(r,this.docIteratorGetMatch());
            }

            finalScore = finalScore +  this.getWeight(i)/this.getSumWeight() *  score;

        }

        return finalScore;

    }

    /**
     *  getScore for the Indri retrieval model when args are not in the doc
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */

    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

        double finalScore = 0.0;

        for(int i=0; i<this.args.size();i++){

            finalScore = finalScore + ((QrySop)(this.args.get(i))).getDefaultScore(r,docid) *
                    this.getWeight(i)/this.getSumWeight();

        }

        return finalScore;

    }

}
