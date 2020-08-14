import java.io.*;

/**
 *  The WAND operator for all retrieval models.
 */

public class QrySopWand extends QrySopWeight{

    /**
     *  Indicates whether the query has a match.
     *  @param r The retrieval model that determines what is a match
     *  @return True if the query matches, otherwise false.
     */
    public boolean docIteratorHasMatch (RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);  //only indri style in #wand
    }


    /**
     *  Get a score for the document that docIteratorHasMatch matched.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */
    public double getScore (RetrievalModel r) throws IOException {

        if(r instanceof RetrievalModelIndri){
            return this.getScoreIndri(r);
        }
        else {
            throw new IllegalArgumentException
                    (r.getClass().getName() + " doesn't support the WAND operator.");
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

                score = score * Math.pow(termScore,this.getWeight(i)/this.getSumWeight());

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
            score = score * Math.pow(((QrySop)this.args.get(i)).getDefaultScore(r,docid), this.getWeight(i)/this.getSumWeight());
        }
        return score;
    }

}
