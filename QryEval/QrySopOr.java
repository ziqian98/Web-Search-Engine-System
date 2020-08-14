/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

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
              (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }

  /**
   *  getScore for the UnrankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {   //useless???
      //System.out.println("dd");
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
    //in QryEavl.java, q.docIteratorHasMatch (model) is called
    //Then in this java file, docIteratorHasMatch is called, the docIteratorHasMatchMin is called
    //In docIteratorHasMatchMin, cache is set. Check cache first
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      double max_score = 0.0;
      //search for all query args like "apple" ,"pie" under #OR
      //to see who have the biggest score
      for (int i=0; i<this.args.size(); i++) {
        //this.args.get(i).docIteratorHasMatch(r), cache is set in (scoreOP, term), getscore in QrySopScore can use HasMatchCache
        if(this.args.get(i).docIteratorHasMatch(r) &&  // "apple" may not match any document  //like used in QryEavl.java, for it's child object
                this.args.get(i).docIteratorGetMatch() == this.docIteratorGetMatch()){
          if(this.args.get(i) instanceof QrySop){
            if(max_score < ((QrySop) this.args.get(i)).getScore(r)){
              max_score = ((QrySop) this.args.get(i)).getScore(r);
              //this.args.get(i) can be QrySopAnd, QrySopOr, QrySopScore.
              //should ues QrySop Here
              //HW1 used QrySopScore, it's wrong
            }
          }
        }
      }

      return max_score;
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

            termScore = ((QrySop)this.args.get(i)).getScore(r);
          }else {
            termScore = ((QrySop)this.args.get(i)).getDefaultScore(r,this.docIteratorGetMatch());
          }

          score = score *(1-termScore);

        }
        return 1-score;
      }

    }


    /**
     *  getDefaultScore for the Intri retrieval model.
     *  @param r The retrieval model that determines how scores are calculated.
     *  @return The document score.
     *  @throws IOException Error accessing the Lucene index
     */

    public double getDefaultScore(RetrievalModel r, long docid)throws IOException{
      double score =1.0;

      for(int i=0; i<args.size();i++){
        score = score * (1-((QrySop)this.args.get(i)).getDefaultScore(r,docid));
      }
      return 1-score;
    }


  }
