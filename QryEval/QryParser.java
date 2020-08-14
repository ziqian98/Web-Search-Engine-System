/*
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.en.EnglishAnalyzerConfigurable;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 *  QryParser is an embarrassingly simplistic query parser.  It has
 *  two primary methods:  getQuery and tokenizeString.  getQuery
 *  converts a query string into an optimized Qry tree.  tokenizeString
 *  converts a flat (unstructured) query string into a string array; it
 *  is used for creating learning-to-rank feature vectors.
 *  <p>
 *  Add new operators to the query parser by modifying the following
 *  methods:
 *  </p>
 *  <ul>
 *  <li>createOperator: Use a string (e.g., #and) to create a node
 *      (e.g., QrySopAnd).
 *
 *  <li>parseString:  If the operator supports term weights
 *      (e.g., #wsum (0.5 apple 1 pie)), you must modify this method.
 *      For these operators, two substrings (weight and term) are
 *      popped from the query string at each step, instead of one.
 *  </ul>
 *  <p>
 *  Add new document fields to the parser by modifying createTerms.
 *  </p>
 */

public class QryParser {

  //  --------------- Constants and variables ---------------------

  private static final EnglishAnalyzerConfigurable ANALYZER =
    new EnglishAnalyzerConfigurable();

  //  -------------------- Initialization -------------------------

  static {
    ANALYZER.setLowercase(true);
    ANALYZER.setStopwordRemoval(true);
    ANALYZER.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }

  //  ----------- Methods, in alphabetical order ------------------

  /**
   *  Count the number of occurrences of character c in string s.
   *  @param c A character.
   *  @param s A string.
   *  @return The number of times c occurs in s.
   */
  private static int countChars (String s, char c) {

    int count=0;

    for (int i=0; i<s.length(); i++) {
      if (s.charAt (i) == c) {
	count ++;
      }
    }

    return count;
  }


  /**
   *  Create the desired query operator.
   *  @param operatorName The operator name.
   *  @return The new query operator
   */
  private static Qry createOperator (String operatorName) {

    Qry operator=null;
    int operatorDistance=0;
    String operatorNameLowerCase = (new String (operatorName)).toLowerCase();

    int num = 0;


    //System.out.println("Before: "+operatorNameLowerCase);

    if(operatorNameLowerCase.contains("/")) {

      String [] substr = operatorNameLowerCase.split("/", 2);

      operatorNameLowerCase = substr[0];

      num = Integer.parseInt(substr[1]);

    }




    //  STUDENTS:: 
    //  Remove the distance argument from proximity operators such as
    //  #near/n and #window/n before trying to create the operator.

    //  Create the query operator.

    switch (operatorNameLowerCase) {
      case "#or":
        operator = new QrySopOr ();
        break;

      case "#syn":
        operator = new QryIopSyn ();
        break;

      case "#and":
        operator = new QrySopAnd ();
        break;

      case "#near":
        operator = new QryIopNear(num);
        break;

      case "#window":
        operator = new QryIopWindow(num);
        break;

      case "#sum":
        operator = new QrySopSum();
        break;

      case "#wand":
        operator = new QrySopWand();
        break;

      case "#wsum":
        operator = new QrySopWsum();
        break;



      //  STUDENTS:: 
      //  Add new query operators here.

      default:
	syntaxError ("Unknown query operator " + operatorName);
    }

    operator.setDisplayName (operatorName);

    return operator;
  }
  
  /**
   *  Create one or more terms from a token.  The token may contain
   *  dashes or other punctuation b(e.g., near-death) and/or a field
   *  name (e.g., apple.title).
   *  @param token The token consumed from the query string.
   *  @return Returns a 1-d array of query operators (one per token, mostly)
   *  @throws IOException Error accessing the Lucene index.
   */
  private static Qry[] createTerms (String token) throws IOException {

    //  Split the token into a term and a field.

    int delimiter = token.indexOf('.');
    String field = null;
    String term = null;

    if (delimiter < 0) {	// .body is the default field
      field = "body";
      term = token;
    } else {			// Remove the field from the token
      field = token.substring(delimiter + 1).toLowerCase();
      term = token.substring(0, delimiter);
    }

    //  Confirm that the field is a known field.

    if ((field.compareTo("url") != 0) &&
	(field.compareTo("keywords") != 0) &&
	(field.compareTo("title") != 0) &&
	(field.compareTo("body") != 0) &&
	(field.compareTo("inlink") != 0)) {
      syntaxError ("Unknown field " + token);
    }

    //  Lexical processing, stopwords, stemming.  A loop is used
    //  just in case a term (e.g., "near-death") gets tokenized into
    //  multiple terms (e.g., "near" and "death").

    String tokens[] = tokenizeString(term);
    Qry terms[] = new Qry[tokens.length];
    
    for (int j = 0; j < tokens.length; j++) {
      terms[j] = new QryIopTerm(tokens [j], field);
    }
    
    return terms;
  }


  /**
   *  Parse a query string into a query tree.
   *  @param queryString The query string, in an Indri-style
   *  query language.
   *  @return Qry The query tree for the parsed query.
   *  @throws IOException Error accessing the Lucene index.
   *  @throws IllegalArgumentException Query syntax error.
   */
  public static Qry getQuery (String queryString)
      throws IOException, IllegalArgumentException {

    Qry q = parseString (queryString);		// An exact parse
    q = optimizeQuery (q);			// An optimized parse
    return q;
  }

  /**
   *  Get the index of the right parenenthesis that balances the
   *  left-most parenthesis.  Return -1 if it doesn't exist.
   *  @param s A string containing a query.
   *  @return The index of the balancing right parenthesis
   */
  private static int indexOfBalencingParen (String s) {

    int depth = 0;

    //System.out.println("In the indexOfBalencingParen:" + s);

    for (int i=0; i< s.length(); i++) {
      if (s.charAt(i) == '(') {
	depth ++;
      } else if (s.charAt(i) == ')') {
	depth --;
            
	if (depth == 0) {
	  return i;
	}
      }
    }

    return -1;
  }


  /**
   *  Optimize the query by removing degenerate nodes produced during
   *  query parsing, for example '#NEAR/1 (of the)' which turns into
   *  '#NEAR/1 ()' after stopwords are removed; and unnecessary nodes
   *  or subtrees, such as #AND (#AND (a)), which can be replaced by
   *  'a'.
   *  @param q The query to be optimized
   *  @return The optimized query
   */
  private static Qry optimizeQuery(Qry q) {

    //  Term operators don't benefit from optimization.

    if (q instanceof QryIopTerm) {
      return q;
    }

    //  Optimization is a depth-first task, so recurse on query
    //  arguments.  This is done in reverse to simplify deleting
    //  query arguments that become null.
    
    for (int i = q.args.size() - 1; i >= 0; i--) {

      Qry q_i_before = q.args.get(i);
      Qry q_i_after = optimizeQuery (q_i_before);

      if (q_i_after == null) {
        q.removeArg(i);			// optimization deleted the arg
      } else {
        if (q_i_before != q_i_after) {
          q.args.set (i, q_i_after);	// optimization changed the arg
        }
      }
    }

    //  If the operator now has no arguments, it is deleted.

    if (q.args.size () == 0) {
      return null;
    }

    //  Only SCORE operators can have a single argument.  Other
    //  query operators that have just one argument are deleted.

    if ((q.args.size() == 1) &&
        (! (q instanceof QrySopScore))) {
      q = q.args.get (0);
    }

    return q;

  }


  /**
   *  Parse a query string into a query tree.
   *  @param queryString The query string, in an Indri-style query
   *  language.
   *  @return Qry The query tree for the parsed query.
   *  @throws IOException Error accessing the Lucene index.
   *  @throws IllegalArgumentException Query syntax error.
   */
  private static Qry parseString (String queryString)
      throws IOException, IllegalArgumentException {

    //  This simple parser is sensitive to parenthensis placement, so
    //  check for basic errors first.

    queryString = queryString.trim ();	// The last character should be ')'

    if ((countChars (queryString, '(') == 0) ||
	(countChars (queryString, '(') != countChars (queryString, ')')) ||
	(indexOfBalencingParen (queryString) != (queryString.length() - 1))) {
      syntaxError ("Missing, unbalanced, or misplaced parentheses");
    }

    //  The query language is prefix-oriented, so the query string can
    //  be processed left to right.  At each step, a substring is
    //  popped from the head (left) of the string, and is converted to
    //  a Qry object that is added to the query tree.  Subqueries are
    //  handled via recursion.

    //  Find the left-most query operator and start the query tree.

    String[] substrings = queryString.split("[(]", 2);  //length is 2
    //#or(#OR(privilege)) --->  #or and  #OR(privilege))
    //#OR(privilege)) ---> #OR and privilege)


    Qry queryTree = createOperator (substrings[0].trim());

    //  Start consuming queryString by removing the query operator and
    //  its terminating ')'.  queryString is always the part of the
    //  query that hasn't been processed yet.
    
    queryString = substrings[1];
    queryString =
      queryString.substring (0, queryString.lastIndexOf(")")).trim();
    
    //  Each pass below handles one argument to the query operator.
    //  Note: An argument can be a token that produces multiple terms
    //  (e.g., "near-death") or a subquery (e.g., "#and (a b c)").
    //  Recurse on subqueries.

    String currentOP = substrings[0].toLowerCase().trim(); // in case of "#wand "
    //System.out.println("currentOP :"+currentOP+"***");

    while (queryString.length() > 0) {
	
      //  STUDENTS:: (HW2)
      //  If the operator uses weighted query arguments (e.g., #WAND),
      //  each pass of this loop must handle "weight arg".  Handle the
      //  weight first.  

      //  Now handle the argument (which could be a subquery).

      double weight = 0.0;

      Qry[] qargs = null;
      PopData<String,String> p;

      //get weight first
      if(currentOP.equals("#wsum") || currentOP.equals("#wand")){
        //System.out.println("here");
        p = popTerm(queryString);
        weight = Double.parseDouble(p.getPopped());
        queryString = p.getRemaining().trim();
      }




      if (queryString.charAt(0) == '#') {	// Subquery
	  p = popSubquery (queryString);
	  qargs = new Qry[1];  //a new subtree
        //System.out.println(qargs[0]);
        //String tes = p.getPopped();
        //System.out.println("hhaa");
	  qargs[0] = parseString (p.getPopped()); //return a new subtree
        //System.out.println("hhaa");

      } else {					// Term
        //System.out.println("is here");
	  p = popTerm (queryString);
	  qargs = createTerms (p.getPopped());  //in the #OR(str1 str2), it will create str1 firstly
      }

      //System.out.println("Before p.getRemaining().trim(): "+queryString);
      queryString = p.getRemaining().trim();	// Consume the arg
      //queryString: str1 str2 ---> str2
      //System.out.println("After p.getRemaining().trim(): "+queryString);


      //  Add the argument(s) to the query tree.

      for (int i=0; i<qargs.length; i++) {
        //  STUDENTS WILL NEED TO ADJUST THIS BLOCK TO HANDLE WEIGHTS IN HW2

	    queryTree.appendArg (qargs[i]);

	    if(currentOP.equals("#wsum")||currentOP.equals("#wand")){
          ((QrySopWeight)queryTree).addWeight(weight);
        }

      }
    }

    return queryTree;
  }  

    
  /**
   *  Remove a subQuery from an argument string.  Return the subquery
   *  and the modified argument string.
   *  @param argString A partial query argument string, e.g., "#and(a b)
   *  c d".
   *  @return PopData&lt;String,String&gt; The subquery string and the
   *  modified argString (e.g., "#and(a b)" and "c d".
   */

  //NOTE : #or(#and(a b) c d)  ---> #and(a b) c d
  //       #or(#OR(privilege))  --->  #OR(privilege)
  static private PopData<String,String> popSubquery (String argString) {
	
    int i = indexOfBalencingParen (argString);
	  
    if (i < 0) {		// Query syntax error.  The parser
      i = argString.length();	// handles it.  Here, just don't fail.
    }

    // for #and(a b) c d
    String subquery = argString.substring(0, i+1);  //#and(a b)
    argString = argString.substring(i+1);  //c d

    return new PopData<String,String>(subquery, argString);
  }

    
  /**
   *  Remove a term from an argument string.  Return the term and
   *  the modified argument string.
   *  @param argString A partial query argument string, e.g., "a b c d".
   *  @return PopData&lt;String,String&gt;
   *  The term string and the modified argString (e.g., "a" and
   *  "b c d".
   */
  static private PopData<String,String> popTerm (String argString) {

    //System.out.println("argString: "+argString);
	
    String[] substrings = argString.split ("[ \t\n\r]+", 2);  //it will split by a space" "!!!
    String token = substrings[0];

    //System.out.println("token is : " + token);

    if (substrings.length < 2) {	//  Is this the last argument?
      argString = "";
    } else {
      argString = substrings[1];
      //System.out.println("substrings[1]"+argString);
    }

    return new PopData<String,String>(substrings[0], argString);
  }

    
  /**
   *  Throw an error specialized for query parsing syntax errors.
   *  @param errorString The string "Syntax
   *  @throws IllegalArgumentException The query contained a syntax
   *  error
   */
  static private void syntaxError (String errorString) throws IllegalArgumentException {
    throw new IllegalArgumentException ("Syntax Error: " + errorString);
  }


  /**
   *  Given part of a query string, returns an array of terms with
   *  stopwords removed and the terms stemmed using the Krovetz
   *  stemmer.  Use this method to process raw query terms.
   *  @param query String containing query. 
   *  @return Array of query tokens
   *  @throws IOException Error accessing the Lucene index.
   */
  public static String[] tokenizeString(String query) throws IOException {


    TokenStream tokenStream = ANALYZER.tokenStream ("dummyField", new StringReader(query));
    CharTermAttribute charTermAttribute =
      tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();

    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }

    tokenStream.close ();

    return tokens.toArray (new String[tokens.size()]);
  }


}
