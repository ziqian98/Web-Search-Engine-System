import java.io.*;
import java.util.*;

/**
 *  The Window/n operator for all retrieval models.
 */


public class QryIopWindow extends QryIop {

    private boolean hasMoreLoc = true;
    private boolean hasMoreDoc = true;

    public QryIopWindow(int wlen){
        this.w_len  = wlen;
    }

    protected void evaluate() {
        //store #Window results
        this.invertedList = new InvList (this.getField());


        if (args.size () == 0) {
            this.invertedList = ((QryIop) args.get(0)).invertedList;
            return;
        }

        while(hasMoreDoc){
            if(this.docIteratorHasMatchAll(null)){  //match a common doc first
                List<Integer> maxLocs = new ArrayList<Integer>();

                int matchedDocid  = this.args.get(0).docIteratorGetMatch();

                //in a single doc, find all qualified max pos
                while(hasMoreLoc){
                    int minpos = -1;
                    int maxpos = -1;
                    int minTermIndex = -1;

                    for(int i=0; i<this.args.size();i++){

                        QryIop q = (QryIop)args.get(i);

                        if(q.locIteratorHasMatch()){

                            int currentPos = q.locIteratorGetMatch();

                            if(maxpos<currentPos || maxpos==-1)
                                maxpos = currentPos;
                            if(minpos>currentPos || minpos==-1){
                                minTermIndex = i;
                                minpos = currentPos;
                            }

                        }else{ //no more match in this doc
                            hasMoreLoc = false;
                            break;
                        }

                    }

                    if(!hasMoreLoc){  //some args reach the end in a doc
                        break;
                    }else if(maxpos-minpos<this.w_len){
                        maxLocs.add(maxpos);
                        for(Qry q_i : this.args){
                            QryIop q = (QryIop) q_i;
                            q.locIteratorAdvance();
                        }
                    }else{
                        ((QryIop)args.get(minTermIndex)).locIteratorAdvance();
                    }
                }

                hasMoreLoc=true;

                if(maxLocs.size()!=0)
                    this.invertedList.appendPosting(matchedDocid,maxLocs);

                for(int j=0; j<this.args.size(); j++){
                    this.args.get(j).docIteratorAdvancePast(matchedDocid);
                }

            }else{  // no matched doc
                hasMoreDoc = false;
            }

        }
    }

}
