import java.io.*;

/**
 *  The AND operator for INDRI retrieval models.
 */

public class QrySopAnd extends QrySop{

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        if(r instanceof RetrievalModelIndri)
            return this.docIteratorHasMatchMin(r);
        return this.docIteratorHasMatchAll(r);
    }


    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean) {
            return this.getScoreUnrankedBoolean (r);
        }

        if (r instanceof  RetrievalModelRankedBoolean) {
            return this.getScoreRankedBoolean(r);
        }

        if(r instanceof RetrievalModelIndri){
            return this.getScoreIndri(r);
        }
        //  STUDENTS::
        //  Add support for other retrieval models here.

        else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the AND operator.");
        }
    }

    /**
     *  getScore for the UnrankedBoolean retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
        if (! this.docIteratorHasMatchCache()) {

            return 0.0;
        } else {
            return 1.0;
        }
    }

    /**
     *  getScore for the RankedBoolean retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */

    private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
        if (! this.docIteratorHasMatchCache()) {
            return 0.0;
        } else {

            double min_score = 0;
            //search for all query args like "apple" ,"pie" under #AND
            //to see who have the smallest score
            for (int i=0; i<this.args.size(); i++) {
                    if(this.args.get(i) instanceof QrySop  ){
                        if(min_score > ((QrySop) this.args.get(i)).getScore(r) || min_score == 0){
                            min_score = ((QrySop) this.args.get(i)).getScore(r);
                            //this.args.get(i) can be QrySopAnd, QrySopOr, QrySopScore.
                            //should ues QrySop Here
                            //HW1 used QrySopScore, it's wrong
                        }
                    }
            }

            return min_score;
        }
    }


    /**
     *  getScore for the Indri retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */

    public double getScoreIndri(RetrievalModel r)throws  IOException{
        if(!this.docIteratorHasMatchCache()){
            return 0.0;
        }else{
            double termScore;
            double score = 1.0;

            for(int i=0;i<args.size();i++){

                if(this.args.get(i).docIteratorHasMatchCache()&&
                        this.docIteratorGetMatch()==this.args.get(i).docIteratorGetMatch()){
                    //term exits in the matched doc
                    //if a real term below this #and, calls the #score getscore to get the score
                    //if the is #or below this #and, calls the #or getScore, and the #or calls its child #score
                    termScore = ((QrySop)this.args.get(i)).getScore(r);
                }else {
                    termScore = ((QrySop)this.args.get(i)).getDefaultScore(r,this.docIteratorGetMatch());
                }

                score = score * Math.pow(termScore,1.0/(double)args.size());

            }
            return score;
        }

    }


    /**
     *  getScore for the Indri retrieval model when args are not in the doc
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */

    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

        double score =1.0;

        for(int i=0; i<args.size();i++){
            score = score * Math.pow(((QrySop)this.args.get(i)).getDefaultScore(r,docid), 1.0/(double)this.args.size());
        }
        return score;
    }
}
