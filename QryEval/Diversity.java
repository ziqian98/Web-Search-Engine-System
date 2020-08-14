import java.io.*;
import java.util.*;

public class Diversity {


    private Map<String, String> parameters;

    Diversity(Map<String, String> parameters){
        this.parameters = parameters;
    }

    public void go()throws Exception{
        DiversityPrepare dp = new DiversityPrepare(this.parameters);

        List<QueryWithDiversity>  q = dp.getAllDiversityQ();

        if(parameters.get("diversity:algorithm").equals("PM2")){
            PM2 p = new PM2(this.parameters);
            p.pm(q);
        }

        if(parameters.get("diversity:algorithm").equals("xQuAD")){
            XQUAD x = new XQUAD(this.parameters);
            x.xq(q);
        }


    }


    public static void norm (List<DocWithDiversity> list){
        double [] scoreForEachColumn = new double[1+list.get(0).getDiversityScoreList().size()];

        for(int i=0; i<list.size(); i++){
            for(int j=0; j<list.get(0).getDiversityScoreList().size()+1;j++){
                if(j!=0)
                    scoreForEachColumn[j] += list.get(i).getDiversityScoreList().get(j-1);
                else
                    scoreForEachColumn[0] += list.get(i).getInitScore();
            }
        }


        double max = 0.0;
        for(int i=0; i<scoreForEachColumn.length;i++){
            if(i==0) {
                max = scoreForEachColumn[i];
                continue;
            }
            if(max<scoreForEachColumn[i])
                max = scoreForEachColumn[i];

        }

        for(int i=0; i<list.size(); i++) {
            list.get(i).normList(max);
            list.get(i).normScore(max);

        }

    }



}
