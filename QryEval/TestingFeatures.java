import java.util.*;
import java.io.*;

public class TestingFeatures {
    public void produceTestFeature(Map<String, String> parameters)throws  Exception{

        PrintWriter featureWriter = new PrintWriter(parameters.get("letor:testingFeatureVectorsFile" ));
        BufferedReader queryReader = new BufferedReader(new FileReader(parameters.get("queryFilePath")));
        int topn;
        double count = 0;
        String eachQ;

        Features feature = new Features(parameters);

        while((eachQ=queryReader.readLine())!=null ){

            String qid = eachQ.substring(0, eachQ.indexOf(":"));
            String query = eachQ.substring(eachQ.indexOf(":"),eachQ.length());


            RetrievalModel model = new RetrievalModelBM25( Double.parseDouble(parameters.get("BM25:b")),
                                    Double.parseDouble(parameters.get("BM25:k_1")),
                                    Double.parseDouble(parameters.get("BM25:k_3")));

            ScoreList top100 = QryEval.processQuery(parameters,qid,query, model);


            if(top100!=null){
                LinkedHashMap<String, double[]> docANDvector = new LinkedHashMap<>();

                if(top100.size()<100)
                    topn = top100.size();
                else
                    topn = 100;

                top100.sort();
                for(int i=0; i<topn; i++){
                    double [] docFeatures = feature.getDocFeatures(QryParser.tokenizeString(query),
                            Idx.getExternalDocid(top100.getDocid(i)));
                    if(docFeatures!=null){
                        docANDvector.put(Idx.getExternalDocid(top100.getDocid(i)) + " " + 0,docFeatures);
                    }
                }

                feature.normalizeFeature(docANDvector, qid,  featureWriter);
            }

            count++;
        }


        featureWriter.close();
        queryReader.close();

        //must close before train!!!

        SVM testSVM = new SVM();
        testSVM.test(parameters);




        //Thread.sleep(5000);

        writeTOteIn(count,parameters);

    }

    public void writeTOteIn (double qcount,Map<String, String> parameters) throws Exception{

        int line = 0;
        int prevQID=0;
        int num = 0;
        int topn=0;

        ScoreList[] slist = new ScoreList[(int)qcount];
        String eachFeature;
        String eachScore;

        //System.out.println(qcount);

        for(int i=0; i<slist.length;i++){
            slist[i] = new ScoreList();
        }

        PrintWriter teInWriter = new PrintWriter(parameters.get("trecEvalOutputPath" ));
        BufferedReader scoreReader = new BufferedReader(new FileReader(parameters.get("letor:testingDocumentScores")));
        BufferedReader featureReader = new BufferedReader(new FileReader(parameters.get("letor:testingFeatureVectorsFile")));

        while ((eachScore = scoreReader.readLine()) != null && (eachFeature = featureReader.readLine()) != null) {
            int qid = Integer.parseInt((eachFeature.split(" ")[1]).split(":")[1]);

            if(line!=0 && prevQID!=qid){
                if(slist[num].size()<100)
                    topn = slist[num].size();
                else
                    topn = 100;
                slist[num].sort();

                for(int i=0; i<topn; i++){
                    teInWriter.print(prevQID+" Q0 " + Idx.getExternalDocid(slist[num].getDocid(i))+" "+
                            (i + 1) + " " + slist[num].getDocidScore(i) + " ziqian\n");
                }

                num++;
                //System.out.println("Do not Reach Here");
            }else {
                //System.out.println(eachScore);
                slist[num].add(Idx.getInternalDocid(eachFeature.split(" ")[eachFeature.split(" ").length - 1]),
                        Double.parseDouble(eachScore));

            }

            //System.out.println("=================================");

            line++;
            prevQID = qid;
        }


        if(slist[num].size()<100)
            topn = slist[num].size();
        else
            topn = 100;
        slist[num].sort();

        for(int i=0; i< topn; i++){
            teInWriter.print(prevQID+" Q0 " + Idx.getExternalDocid(slist[num].getDocid(i))+" "+
                    (i + 1) + " " + slist[num].getDocidScore(i) + " ziqian\n");
        }

        scoreReader.close();
        featureReader.close();
        teInWriter.close();

    }
}
