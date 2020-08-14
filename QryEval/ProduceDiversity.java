import java.util.*;
import java.io.*;

public class ProduceDiversity {

    private Map<String, String> parameters;

    ProduceDiversity(Map<String, String> parameters){
        this.parameters = parameters;
    }


    public Map<String, List<DocWithDiversity>> produce() throws IOException{
        Map<String, List<DocWithDiversity>> result = new HashMap<>();
        Map<String,Map<String,Double>>  intentExidScoreMap = new HashMap<>();
        BufferedReader initReader = new BufferedReader(new FileReader(this.parameters.get("diversity:initialRankingFile")));
        Map<String, Double> eidANDscore;
        List<DocWithDiversity> docList;
        String prevQID = "";

        String eachLine = null;

        while((eachLine=initReader.readLine())!=null){
            double score = Double.parseDouble(eachLine.split(" ")[4]);
            //System.out.println(score);
            String externalID = eachLine.split(" ")[2];
            String qid = eachLine.split(" ")[0];

            if(qid.contains(".")){
                if(!(prevQID.equals(qid))){
                    eidANDscore = new HashMap<>();
                    eidANDscore.put(externalID,score);
                    intentExidScoreMap.put(qid,eidANDscore);

                }else {
                    eidANDscore = intentExidScoreMap.get(qid);
                    if(eidANDscore.size()>=Integer.parseInt(this.parameters.get("diversity:maxInputRankingsLength")))
                        continue;
                    eidANDscore.put(externalID,score);
                    intentExidScoreMap.put(qid,eidANDscore);

                }

                prevQID = qid;

            }else{
                if(!(prevQID.equals(qid))){
                    docList = new ArrayList<>();
                    docList.add(new DocWithDiversity(externalID,score));
                    result.put(qid,docList);
                }else {
                    docList = result.get(qid);
                    if(docList.size()>= Integer.parseInt(this.parameters.get("diversity:maxInputRankingsLength")) )
                        continue;
                    docList.add(new DocWithDiversity(externalID,score));
                    result.put(qid,docList);

                }

                prevQID = qid;

            }

        }


        for(Map.Entry<String, List<DocWithDiversity>> entry : result.entrySet()) {

            docList = result.get(entry.getKey());
            boolean neednorm = false;
            Map<String, Double> exidScoreMap;

            //System.out.println("docList.size(): "+docList.size());

            for(DocWithDiversity eachDoc : docList){
                int num = 1;
                if(eachDoc.getInitScore()>=1)
                    neednorm = true;

                while (true){

                    exidScoreMap = intentExidScoreMap.get(entry.getKey()+"."+num);

                    num++;

                    if(exidScoreMap == null)
                        break;

                    if( ( exidScoreMap.get(eachDoc.getExternalID()) )!= null ){

                        if(exidScoreMap.get(eachDoc.getExternalID())>1)
                            neednorm = true;
                        eachDoc.incrementDiversityScoreList(exidScoreMap.get(eachDoc.getExternalID()));
                    }else{
                        eachDoc.incrementDiversityScoreList(0.0);
                    }

                }
            }

            if(neednorm)
                Diversity.norm(docList);

        }

        initReader.close();
        return result;
    }


}
