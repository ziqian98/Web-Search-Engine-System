/*
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.4.
 *
 *  Compatible with Lucene 8.1.1.
 */
import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.Map.Entry;


import org.apache.lucene.index.*;

import javax.swing.*;

/**
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a guide for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
    "Usage:  java QryEval paramFile\n\n";

  private static boolean needQueryExpansion = false;

  //  --------------- Methods ---------------------------------------

  /**
   *  @param args The only argument is the parameter file name.
   *  @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  This is a timer that you may find useful.  It is used here to
    //  time how long the entire program takes, but you can move it
    //  around to time specific parts of your code.

    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE); //java.lang.IllegalArgumentException
    }

    Map<String, String> parameters = readParameterFile (args[0]);

    //  Open the index and initialize the retrieval model.

    Idx.open (parameters.get ("indexPath"));


    if (parameters.containsKey("retrievalAlgorithm") && parameters.get("retrievalAlgorithm").toLowerCase().equals("letor")  ) {
      Letor letorModel = new Letor(parameters);
      letorModel.go();

    }else if(parameters.containsKey("diversity") && parameters.get("diversity").toLowerCase().equals("true")){

      Diversity diversity = new Diversity(parameters);
      diversity.go();



    }else{

    RetrievalModel model = initializeRetrievalModel (parameters);

    //  Perform experiments.

    processQueryFile(parameters, model);
    }


    //  Clean up.

    timer.stop ();
    System.out.println ("Time:  " + timer);
  }

  /**
   *  Allocate the retrieval model and initialize it using parameters
   *  from the parameter file.
   *  @param parameters All of the parameters contained in the parameter file
   *  @return The initialized retrieval model
   *  @throws IOException Error accessing the Lucene index.
   */
  public static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
    throws IOException {

    RetrievalModel model = null;
    String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();  //equals() is case sensitive

    if (modelString.equals("unrankedboolean")) {

      model = new RetrievalModelUnrankedBoolean();

      //  If this retrieval model had parameters, they would be
      //  initialized here.

    }else if(modelString.equals("rankedboolean")){
      model = new RetrievalModelRankedBoolean();
    }else if(modelString.equals("bm25")){
      double b = Double.parseDouble(parameters.get("BM25:b"));
      double k1 = Double.parseDouble(parameters.get("BM25:k_1"));
      double k3 = Double.parseDouble(parameters.get("BM25:k_3"));

      model = new RetrievalModelBM25(b,k1,k3);
    }else if(modelString.equals("indri")){
      double miu = Double.parseDouble(parameters.get("Indri:mu"));
      double lambda = Double.parseDouble(parameters.get("Indri:lambda"));

      model = new RetrievalModelIndri(miu,lambda);
    }

    //  STUDENTS::  Add new retrieval models here.

    else {

      throw new IllegalArgumentException
        ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
    }

    return model;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can
   * indicate whether garbage collection should be performed, which slows the
   * program but reduces memory usage.
   *
   * @param gc
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    System.out.println("Memory used:  "
        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Process one query.
   * @param qryString A string that contains a query.
   * @param model The retrieval model determines how matching and scoring is done.
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static ScoreList processQuery(Map<String, String> parameters, String qid, String qryString, RetrievalModel model)
    throws Exception {

    String defaultOp = model.defaultQrySopName ();
    //System.out.println("defaultOp: "+defaultOp);
    qryString = defaultOp + "(" + qryString + ")";
    System.out.println("qryString: "+qryString);

    Map<String, ScoreList> initialRanking = null;

    Qry q = null;

    if(parameters.containsKey("fb")&&parameters.get("fb").equals("true")){
      if(parameters.containsKey("fbInitialRankingFile")){  //has InitialRankingFile in param
        initialRanking = readFBrank(parameters.get("fbInitialRankingFile"));  //get initial ranking
        //System.out.println("KKK");
      }
      needQueryExpansion = true;
    }

    if(needQueryExpansion){  // need to do query expansion

      ScoreList originalQueryResults = new ScoreList();

      if(initialRanking!=null){
        originalQueryResults = initialRanking.get(qid);
      }else { // need to run initial query to get the initial ranking score


        q = QryParser.getQuery(qryString);

        if (q != null) {
          if (q.args.size() > 0) {        // Ignore empty queries
            q.initialize(model);
            while (q.docIteratorHasMatch(model)) {  //NOTE : q.docIterator is different for each loop
              int originaldocid = q.docIteratorGetMatch();   //match a doc first. In #OR, this is a MinDocID
              double originalscore = ((QrySop) q).getScore(model);   //then calculate the score
              originalQueryResults.add(originaldocid, originalscore);     //NOTE: each loop add a doc and its CORRESPONDING score
              q.docIteratorAdvancePast(originaldocid);  //NOTE: q.docIterator go PAST the current docid
            }
          }

        }
      }


      originalQueryResults.sort();  //remember to sort!!!

      double fbMu = Double.parseDouble(parameters.get("fbMu"));
      double fbOrigWeight = Double.parseDouble(parameters.get("fbOrigWeight"));
      int fbTerms = Integer.parseInt(parameters.get("fbTerms"));
      int fbDocs = Integer.parseInt(parameters.get("fbDocs"));


      originalQueryResults.truncate(fbDocs);
//      for(int i=0; i<originalQueryResults.size();i++){
//        System.out.println("top " + i + "score: "+originalQueryResults.getDocidScore(i));
//      }
      String expandedQuery = getExpandedQuery(fbTerms, fbMu, originalQueryResults);

      System.out.println("expandedQuery: "+ expandedQuery);

      //write the expanded query to specified file
      BufferedWriter w = new BufferedWriter(new FileWriter(parameters.get("fbExpansionQueryFile"),true));
      w.write(qid+": "+expandedQuery+"\n");
      w.close();  //failed to write without this closing

      //get the final combined query

      StringBuilder finalQ = new StringBuilder("#wand (");
      finalQ.append(fbOrigWeight);
      finalQ.append(" ");
      finalQ.append(qryString);
      finalQ.append(" ");
      finalQ.append(1-fbOrigWeight);
      finalQ.append(" ");
      finalQ.append(expandedQuery);
      finalQ.append(")");

      String finalQuery = finalQ.toString();

      q = QryParser.getQuery(finalQuery);

    }else{
     q = QryParser.getQuery (qryString);
    }


    // Show the query that is evaluated

    System.out.println("    --> " + q);

    if (q != null) {

      ScoreList results = new ScoreList ();

      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model);

        while (q.docIteratorHasMatch (model)) {  //NOTE : q.docIterator is different for each loop
          int docid = q.docIteratorGetMatch ();   //match a doc first. In #OR, this is a MinDocID
          double score = ((QrySop) q).getScore (model);   //then calculate the score
          results.add (docid, score);     //NOTE: each loop add a doc and its CORRESPONDING score
          q.docIteratorAdvancePast (docid);  //NOTE: q.docIterator go PAST the current docid
        }
      }

      return results;
    } else
      return null;
  }

  /**
   *  Process the query file.
   *  @param parameters To store parafile information
   *  @param model A retrieval model that will guide matching and scoring
   *  @throws IOException Error accessing the Lucene index.
   */
  static void processQueryFile(Map<String, String> parameters,
                               RetrievalModel model)
      throws Exception {

    String queryFilePath =  parameters.get("queryFilePath");

    BufferedReader input = null;

    try {
      String qLine = null;

      input = new BufferedReader(new FileReader(queryFilePath));

      //  Each pass of the loop processes one query.

      while ((qLine = input.readLine()) != null) {

        printMemoryUsage(false);
        System.out.println("Query " + qLine);
	String[] pair = qLine.split(":");

	if (pair.length != 2) {
          throw new IllegalArgumentException
            ("Syntax error:  Each line must contain one ':'.");
	}

	String qid = pair[0];
	String query = pair[1];
        ScoreList results = processQuery(parameters,qid, query, model);

        if (results != null) {
          printResults(qid, results,parameters.get("trecEvalOutputPath"),
                  Integer.parseInt(parameters.get("trecEvalOutputLength")));
          System.out.println();
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      input.close();
    }
  }

  /**
   * Print the query results.
   *
   * STUDENTS::
   * This is not the correct output format. You must change this method so
   * that it outputs in the format specified in the homework page, which is:
   *
   * QueryID Q0 DocID Rank Score RunID
   *
   * @param queryName
   *          Original query.
   * @param result
   *          A list of document ids and scores
   * @param trecEvalOutputLength
   *          Max output length for a single doc
   * @param trecEvalOutputPath
   *          Output path
   * @throws IOException Error accessing the Lucene index.
   *
   */
  static void printResults(String queryName, ScoreList result, String trecEvalOutputPath, int trecEvalOutputLength) throws IOException {
    //queryName is qid

    result.sort();

    FileWriter fw = new FileWriter(new File(trecEvalOutputPath),true);

    BufferedWriter  bw = new BufferedWriter(fw);

    //System.out.println(queryName + ":  ");
    if (result.size() < 1) {
      bw.write(queryName + "\t" + 0 + "\t" + "dummy" + "\t"
              + 1 + "\t" + 0 + "\t" +"run-1"+"\n");
      //System.out.println("\tNo results.");
    } else {
      for (int i = 0; i < result.size(); i++) {
        //System.out.println(queryName + "\t" + 0 + "\t" + Idx.getExternalDocid(result.getDocid(i)) + "\t"
           // + (i+1) + "\t" + result.getDocidScore(i) + "\t" +"run-1");

        if(i<trecEvalOutputLength)
        bw.write(queryName + "\t" + 0 + "\t" + Idx.getExternalDocid(result.getDocid(i)) + "\t"
                + (i+1) + "\t" + result.getDocidScore(i) + "\t" +"run-1"+"\n");

      }
    }

    bw.close();

  }

  /**
   *  Read the specified parameter file, and confirm that the required
   *  parameters are present.  The parameters are returned in a
   *  HashMap.  The caller (or its minions) are responsible for processing
   *  them.
   *  @param parameterFileName The name of the parameter file
   *  @return The parameters, in &lt;key, value&gt; format.
   *  @throws IllegalArgumentException The parameter file can't be read or doesn't contain required parameters
   *  @throws IOException The parameter file can't be read
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
    throws IOException {

    Map<String, String> parameters = new HashMap<String, String>();
    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
        ("Can't read " + parameterFileName);
    }

    //  Store (all) key/value parameters in a hashmap.

    Scanner scan = new Scanner(parameterFile);
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split ("=");
      parameters.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    scan.close();

    //  Confirm that some of the essential parameters are present.
    //  This list is not complete.  It is just intended to catch silly
    //  errors.

    if (! (parameters.containsKey ("indexPath") &&
           parameters.containsKey ("queryFilePath") &&
           parameters.containsKey ("trecEvalOutputPath"))) {
      throw new IllegalArgumentException
        ("Required parameters were missing from the parameter file.");
    }

    return parameters;
  }



  private static Map<String, Double> sortByDescValues(Map<String, Double> unsortedMap, int fbterms){
    List<Entry<String, Double>> entryList = new LinkedList<Entry<String, Double>>(unsortedMap.entrySet());

     Collections.sort(entryList, new Comparator<Entry<String, Double>>(){
       public int compare(Entry<String, Double> o1, Entry<String, Double> o2 ){
        return o2.getValue().compareTo(o1.getValue());
      }
    });

     Map<String, Double> sortedMap = new LinkedHashMap<String,Double>();
     int i=0;
     for(Entry<String,Double> entry : entryList){
       i++;
       sortedMap.put(entry.getKey(),entry.getValue());
       if(i>(fbterms-1))
         break;
     }

     return sortedMap;
  }


  private static String getExpandedQuery(int fbTerms, double fbMu, ScoreList truncatedQueryResults) throws IOException{
    StringBuilder expQ = new StringBuilder("#wand (");

    Map<String, List<Integer>> allTermsInvertedList = new HashMap<>();
    Map<String, Double> allTermScore = new HashMap<>();

    double corpusLength = Idx.getSumOfFieldLengths("body");
    //int count =0;

    //find all candidate terms in top docs first
    for(int i=0; i<truncatedQueryResults.size();i++){
      int docid  = truncatedQueryResults.getDocid(i);
      TermVector docAllTerms = new TermVector(docid,"body");

      for(int j=0; j<docAllTerms.stemsLength();j++){

//        if(j==0){
//          System.out.println("Here "+docAllTerms.stemString(j));
//
//          if(docAllTerms.stemString(j)==null){
//            System.out.println("Heree "+docAllTerms.stemString(j));
//          }
//          System.out.println(j);
//        }

        //The 0'th stem indicates a stopword. null
        String eachTerm = docAllTerms.stemString(j);
        //System.out.println("here "+eachTerm);


        //ignore terms contain . , null
        if( (eachTerm != null) && !(eachTerm.contains(".")) && !(eachTerm.contains(",")) ) {

          if(allTermsInvertedList.containsKey(eachTerm)){
            allTermsInvertedList.get(eachTerm).add(docid); //no duplicated docid in each term's doc list
          }else{
            List<Integer> termDocList = new ArrayList<>();
            termDocList.add(docid);
            allTermsInvertedList.put(eachTerm,termDocList);
            //count++;
          }


        }
      }
    }

//    for(String eachTerm : allTermsInvertedList.keySet()){
//      System.out.println(eachTerm);
//      System.out.println(count);
//    }

    //for each candidate term, sum scores for all top docs
    for(String eachTerm : allTermsInvertedList.keySet()){

      List<Integer> termDocList = allTermsInvertedList.get(eachTerm);

      double Ptd=0.0;
      double ctf = 0.0;
      double tf = 0.0;

      for(int i=0; i<truncatedQueryResults.size();i++) {  //calculate score for each doc

        int docid = truncatedQueryResults.getDocid(i);
        TermVector docAllTerms = new TermVector(docid, "body");

        int termIndex = docAllTerms.indexOfStem(eachTerm);


        if (termIndex == -1) {  //if this term does not appear in doci
          tf = 0.0;
          ctf = Idx.getTotalTermFreq("body", eachTerm);

        } else {
          tf = docAllTerms.stemFreq(termIndex);  //get tf for the term existed in doci
          ctf = Idx.getTotalTermFreq("body", eachTerm);
        }


        long doclen = Idx.getFieldLength("body", docid);
        double docOriginalScore = truncatedQueryResults.getDocidScore(i);
        double mleProb = ctf / corpusLength;
        double tfPart = (tf + fbMu * mleProb) / (doclen + fbMu);
        double idfPart = Math.log(corpusLength / ctf);


        Ptd = Ptd + tfPart * docOriginalScore * idfPart;

      }

      allTermScore.put(eachTerm,Ptd);

    }

    Map<String, Double> sortedTopTerms = sortByDescValues(allTermScore,fbTerms);

    for(Map.Entry<String, Double> entry : sortedTopTerms.entrySet()){
      //System.out.println("Each Top Term: "+entry.getKey());
      //System.out.println("Each Top Score: "+entry.getValue());
      expQ.append(String.format("%.4f %s ", entry.getValue(),entry.getKey()));
    }

    expQ.append(")");

    return expQ.toString();

  }

  private static Map<String, ScoreList> readFBrank(String path)throws Exception{
    BufferedReader input = new BufferedReader(new FileReader(path));

    boolean firstLine = true;
    String prevID = null;
    String eachLine = null;

    Map<String, ScoreList> qidANDscorelist = new HashMap<>();

    ScoreList QIDscorelist = new ScoreList();

    while((eachLine = input.readLine())!=null){
      if(eachLine.contains("dummy")){
        continue;
      }

//      if(eachLine.trim().isEmpty()){
//        System.out.println("Should not be empty!!!");
//      }

      String [] columns = eachLine.split("[\\s]+");

      String qid = columns[0].trim();
      String exid = columns[2].trim();
      String scoreString = columns[4].trim();
      double score  = Double.parseDouble(scoreString);

      int docid = Idx.getInternalDocid(exid);

      if(firstLine){  //first line
        prevID = qid;
        firstLine = false;
        QIDscorelist.add(docid,score);
      }else if(qid.equals(prevID)){ //same qid
        QIDscorelist.add(docid,score);
      }else{ //differnet qid
        ScoreList copy = new ScoreList();
        int size = QIDscorelist.size();
        while(size>0){ //inverse results but does not matter
          size--;
          copy.add(QIDscorelist.getDocid(size),QIDscorelist.getDocidScore(size));
        }
        qidANDscorelist.put(prevID,copy);
        QIDscorelist.clear();
        //System.out.println(QIDscorelist.size());
        QIDscorelist.add(docid,score);
      }

      prevID = qid;

    }

    //put the last qid
    ScoreList copy = new ScoreList();
    int size = QIDscorelist.size();
    while(size>0){
      size--;
      copy.add(QIDscorelist.getDocid(size),QIDscorelist.getDocidScore(size));
    }

    qidANDscorelist.put(prevID,copy);

    input.close();

    return qidANDscorelist;

  }



}
