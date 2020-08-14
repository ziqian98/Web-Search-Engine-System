/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
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

    if (r instanceof RetrievalModelRankedBoolean){
      return this.getScoreRankedBoolean (r);
    }

    if(r instanceof  RetrievalModelBM25){
      return this.getScoreBM25(r);
    }

    if(r instanceof RetrievalModelIndri){
      return this.getScoreIndri(r);
    }

    //  STUDENTS::
    //  Add support for other retrieval models here.

    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
    }
  }
  
  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   *  getScore for the Ranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {  //"apple" and "pie", if "apple" has no match, we
      return 0.0;               //cannot find docid in cache. "apple" get 0.0 score
    } else {
      double score = 0.0;
      Qry qry =  this.args.get(0);  // "this" refers to #Score
      if(qry instanceof QryIop) {
        QryIop q_term = (QryIop) qry;
        score = q_term.docIteratorGetMatchPosting().tf;
      }

      return score;

    }
  }


  /**
   *  getScore for the Okapi BM25 retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */

  public double getScoreBM25(RetrievalModel r)throws IOException{
    double score = 0.0;
    if(this.docIteratorHasMatchCache()){

      QryIop q = (QryIop) this.args.get(0);
      String field = q.getField();


      double N = (double)Idx.getNumDocs(); //return long
      double df = (double)q.getDf();  //return int

      double idfWeight = Math.log((N-df+0.5)/(df+0.5));

      double b = ((RetrievalModelBM25) r).getb();
      double tf = (double) q.docIteratorGetMatchPosting().tf;
      double k1 = ((RetrievalModelBM25) r).getk1();
      double doclen = Idx.getFieldLength(field,this.docIteratorGetMatch());
      double avgdoclen =((double)Idx.getSumOfFieldLengths(field))/((double)Idx.getDocCount(field));

      double tfweight = tf/(tf+k1*( (1-b) + b*(doclen/avgdoclen) ) ) ;

      double userweight = 1.0;  //no duplicate term

      score = idfWeight*tfweight*userweight;

      return score;

    }
    return score;

  }

  /**
   *  getScore for the Intri retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */

  public double getScoreIndri(RetrievalModel r)throws IOException{
    double ctf = (double) (((QryIop)this.args.get(0)).getCtf());
    //ctf must > 0 since term occurs in a doc.
    double lambda = ((RetrievalModelIndri) r).getLambda();
    double miu = ((RetrievalModelIndri) r).getMiu();
    double tf = (double) (((QryIop)this.args.get(0)).docIteratorGetMatchPosting().tf);
    double doclen = (double)(Idx.getFieldLength(((QryIop) this.args.get(0)).getField(),this.docIteratorGetMatch()));
    double totalDocLen = Idx.getSumOfFieldLengths(((QryIop) this.args.get(0)).getField());

    double pMLE = ctf/totalDocLen;
    if((1-lambda)*( (tf+miu*pMLE)/(doclen+miu) ) + lambda*pMLE > 0.99)
      System.out.println((1-lambda)*( (tf+miu*pMLE)/(doclen+miu) ) + lambda*pMLE);

    return (1-lambda)*( (tf+miu*pMLE)/(doclen+miu) ) + lambda*pMLE;

  }


  /**
   *  getDefaultScore for the Intri retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */

  public double getDefaultScore(RetrievalModel r, long docid)throws IOException{
    double ctf = (double) (((QryIop)this.args.get(0)).getCtf());
    if(ctf == 0)
      ctf=0.5;
    double lambda = ((RetrievalModelIndri) r).getLambda();
    double miu = ((RetrievalModelIndri) r).getMiu();
    double tf = 0.0;
    double doclen = (double)(Idx.getFieldLength(((QryIop) this.args.get(0)).getField(),(int) docid));
    double totalDocLen = (double)(Idx.getSumOfFieldLengths(((QryIop) this.args.get(0)).getField()));

    double pMLE = ctf/totalDocLen;

    return (1-lambda)*( (tf+miu*pMLE)/(doclen+miu) ) + lambda*pMLE;
  }




  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
