

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class XQUAD {

    private Map<String, String> parameters;


    XQUAD(Map<String, String> parameters){
        this.parameters = parameters;
    }

    public void xq(List<QueryWithDiversity>  q) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(parameters.get("trecEvalOutputPath")));


        for(int qi=0; qi<q.size();qi++){
            ArrayList<DocWithDiversity> selectedDocList = new ArrayList<>();
            List<DocWithDiversity>  docList;

            int maxDocPos = -1;
            int num = Integer.parseInt(this.parameters.get("diversity:maxResultRankingLength"));
            double max = Double.NEGATIVE_INFINITY;

            if(this.parameters.containsKey("diversity:initialRankingFile")){
                ProduceDiversity pd = new ProduceDiversity(this.parameters);
                docList = new LinkedList<>(pd.produce().get(q.get(qi).getQid()));
                //System.out.println(docList.size());
            }else{
                InitDiversity id = new InitDiversity(this.parameters);
                docList =  id.getDiversity(q.get(qi));
                if(parameters.get("retrievalAlgorithm").equals("BM25"))
                    Diversity.norm(docList);
                docList = new LinkedList<>(docList);
            }

            while( (docList.size()>0) && (num>0)){
                num--;

                for(int i=0; i<docList.size();i++){
                    double score = 0.0;
                    for(int j=0; j<q.get(qi).getDiversityQlist().size();j++){
                        //System.out.println(docList.get(i).getDiversityScoreList().size());
                        double coverage = (1.0/ (q.get(qi).getDiversityQlist().size()) )*
                                (docList.get(i).getDiversityScoreList().get(j));

                        for(DocWithDiversity s : selectedDocList)
                            coverage = coverage * (1.0 - s.getDiversityScoreList().get(j));

                        score += coverage;

                    }

                    score = (1.0-Double.parseDouble(this.parameters.get("diversity:lambda"))) *
                            docList.get(i).getInitScore() +
                            (Double.parseDouble(this.parameters.get("diversity:lambda"))) * score;

                    if(score>max){
                        maxDocPos = i;
                        max = score;
                    }
                }

                DocWithDiversity selectedDoc = ((LinkedList<DocWithDiversity>) docList).remove(maxDocPos);
                selectedDoc.setInitScore(max);
                max = Double.NEGATIVE_INFINITY;
                selectedDocList.add(selectedDoc);
            }


            Collections.sort(selectedDocList, new Comparator<DocWithDiversity>() {
                @Override
                public int compare(DocWithDiversity o1, DocWithDiversity o2) {
                    //System.out.println("+++");
                    if(o1.getInitScore()<o2.getInitScore())
                        return 1;
                    else
                        return -1;
                }
            });


            for(int i=0; i< selectedDocList.size();i++){
                if(i< selectedDocList.size()-1)
                    if(selectedDocList.get(i).getInitScore()<selectedDocList.get(i+1).getInitScore())
                        System.out.println("need sort");
                out.write(q.get(qi).getQid() + " Q0 " + selectedDocList.get(i).getExternalID() +
                        " " + (i+1) + " " +  selectedDocList.get(i).getInitScore() + " reference\n");
            }

        }

        out.close();
    }
}
