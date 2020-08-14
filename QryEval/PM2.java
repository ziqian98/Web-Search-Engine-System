import javax.annotation.processing.SupportedSourceVersion;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

public class PM2 {


    private Map<String, String> parameters;

    PM2(Map<String, String> parameters){
        this.parameters = parameters;
    }


    public void pm(List<QueryWithDiversity>  q) throws  Exception{

        BufferedWriter out = new BufferedWriter(new FileWriter(parameters.get("trecEvalOutputPath")));

        for(int qi=0; qi<q.size();qi++){
            ArrayList<DocWithDiversity> selectedDocList = new ArrayList<>();
            List<DocWithDiversity>  docList;

            int maxDocPos = -1;
            int num = Integer.parseInt(this.parameters.get("diversity:maxResultRankingLength"));
            double max = Double.NEGATIVE_INFINITY;
            double v = ((double)(num)) / (q.get(qi).getDiversityQlist().size());
            double []qt = new double[q.get(qi).getDiversityQlist().size()];
            double []s = new double[q.get(qi).getDiversityQlist().size()];
            Arrays.fill(qt,0);
            Arrays.fill(s,0);

            if(this.parameters.containsKey("diversity:initialRankingFile")){
                ProduceDiversity pd = new ProduceDiversity(this.parameters);
                docList = new LinkedList<>(pd.produce().get(q.get(qi).getQid()));

            }else{
                InitDiversity id = new InitDiversity(this.parameters);
                docList =  id.getDiversity(q.get(qi));
                if(parameters.get("retrievalAlgorithm").equals("BM25"))
                    Diversity.norm(docList);
                docList = new LinkedList<>(docList);
            }

            while( (docList.size()>0) && (num>0)) {
                num--;
                int selectedIntent = 0;
                double total = 0;
                for(int i=0; i<q.get(qi).getDiversityQlist().size();i++){
                    qt[i] = v/(2*s[i] + 1);
                }
                for(int i=0; i<q.get(qi).getDiversityQlist().size();i++){
                    if(qt[i] > qt[selectedIntent])   //pick the most first one when tie
                        selectedIntent = i;
                }


                for(int i=0; i<docList.size();i++) {
                    double score = 0.0;
                    for (int j = 0; j < q.get(qi).getDiversityQlist().size(); j++) {

                        if(j!=selectedIntent)
                            score = score + (1-Double.parseDouble(this.parameters.get("diversity:lambda"))) * qt[j]
                                    * docList.get(i).getDiversityScoreList().get(j);
                        else
                            score = score + Double.parseDouble(this.parameters.get("diversity:lambda")) * qt[selectedIntent]
                                    * docList.get(i).getDiversityScoreList().get(selectedIntent);
                    }

                    if(score>max){
                        maxDocPos = i;
                        max = score;
                    }

                }

                DocWithDiversity selectedDoc = ((LinkedList<DocWithDiversity>) docList).remove(maxDocPos);
                //System.out.println("000");
                //System.out.println(max);
                selectedDoc.setInitScore(max);
                max = Double.NEGATIVE_INFINITY;
                selectedDocList.add(selectedDoc);

                for(int i=0; i<selectedDoc.getDiversityScoreList().size();i++){
                    total = total + selectedDoc.getDiversityScoreList().get(i);
                }

                for(int i=0;i<selectedDoc.getDiversityScoreList().size();i++){
                    s[i] = s[i]+ selectedDoc.getDiversityScoreList().get(i)/total;
                }


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
                        " " + (i+1) + " " + selectedDocList.get(i).getInitScore() + " reference\n");
            }


        }
        out.close();

    }

}

