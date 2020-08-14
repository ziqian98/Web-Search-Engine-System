import java.io.*;
import java.util.*;

/**
 *  The NEAR/n operator for all retrieval models.
 */


public class QryIopNear extends QryIop {

    public QryIopNear (int num){
        this.n = num;
    }

    protected void evaluate () throws IOException {
        //  Create an empty inverted list.  If there are no query arguments,
        //  this is the final result.

        //store #Near results
        this.invertedList = new InvList (this.getField());


        if (args.size () == 0) {  //arguments of #Near
        this.invertedList = ((QryIop) args.get(0)).invertedList;
            return;
        }

        //  Each pass of the loop adds 1 document to result inverted list
        //  until all of the argument inverted lists are depleted.

        while(true){

            // Find the common document id.  If there is none, we're done.

                if (this.docIteratorHasMatchAll(null)){

                    List<Integer> mergedCommonLoc =  new ArrayList<Integer>();

                    int commonDocid = this.args.get(0).docIteratorGetMatch();

                    //  Create a new posting to store common loc in a doc

                    Vector<Integer> locations_i =
                            ((QryIop) this.args.get(0)).docIteratorGetMatchPosting().positions;  //get current doc's posting info
                    mergedCommonLoc.clear();
                    mergedCommonLoc.addAll(locations_i);


                    //Implement #Near on each two inverted lists
                    //combine previous processed inverted lists to the next single list, all in the common doc matched before
                    for (int i = 1; i < this.args.size(); i++) {
                        int mergedLoc = 0;  //current loc in the merged inverted list
                        int len = mergedCommonLoc.size();
                        List<Integer> commonLoc = new ArrayList<Integer>();  //to store common locations, gradually small

                        QryIop termToMerge = (QryIop) this.args.get(i);

                        while (mergedLoc < len && termToMerge.locIteratorHasMatch()) {
                            int locToMerge = termToMerge.locIteratorGetMatch();
                            if (mergedCommonLoc.get(mergedLoc) < locToMerge) {
                                if (locToMerge - mergedCommonLoc.get(mergedLoc) <= this.n) {  //matched
                                    //System.out.println("n is "+this.n);
                                    mergedLoc++;
                                    termToMerge.locIteratorAdvancePast(locToMerge);
                                    commonLoc.add(locToMerge);
                                } else {
                                    mergedLoc++;  //usually advance the left one when distance does not match
                                }

                            } else {
                                termToMerge.locIteratorAdvancePast(locToMerge);
                            }
                        }
                        mergedCommonLoc = commonLoc;
                    }

                    if (mergedCommonLoc.size()!=0) {
                        this.invertedList.appendPosting(commonDocid, mergedCommonLoc);
                    }


                    for(int j=0; j<this.args.size(); j++){
                        this.args.get(j).docIteratorAdvancePast(commonDocid);
                    }

                } else {
                    break; //no common doc, no match for near, only exit of while loop
                }

        }

    }
}
