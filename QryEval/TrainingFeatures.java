import java.util.*;
import java.io.*;


public class TrainingFeatures  {
    public void produceTrainFeature(Map<String, String> parameters)throws Exception{

        PrintWriter featureWriter = new PrintWriter(parameters.get("letor:trainingFeatureVectorsFile" ));
        BufferedReader queryReader = new BufferedReader(new FileReader(parameters.get("letor:trainingQueryFile")));
        BufferedReader documentReader = new BufferedReader(new FileReader(parameters.get("letor:trainingQrelsFile")));

        String eachQ;

        Features feature = new Features(parameters);

        while((eachQ=queryReader.readLine())!=null ){

            String qid = eachQ.substring(0, eachQ.indexOf(":"));
            String query = eachQ.substring(eachQ.indexOf(":"),eachQ.length());

            String eachDoc;

            LinkedHashMap<String, double[]> docANDvector = new LinkedHashMap<>();

            while((eachDoc=documentReader.readLine())!=null){
                String [] eachDocSplit = eachDoc.split(" ");
                if(!(eachDocSplit[0].equals(qid)))
                    break;

                double [] docFeatures =feature.getDocFeatures(QryParser.tokenizeString(query),eachDocSplit[2]);

                if(docFeatures!=null)
                    docANDvector.put(eachDocSplit[2]+" "+eachDocSplit[3],
                            docFeatures);
            }

            feature.normalizeFeature(docANDvector, qid,  featureWriter);

        }

        documentReader.close();
        queryReader.close();
        featureWriter.close();

        //must close before test!!!

        SVM trainSVM = new SVM();
        trainSVM.train(parameters);


    }
}
