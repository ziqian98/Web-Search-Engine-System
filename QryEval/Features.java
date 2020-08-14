import java.io.*;
import java.util.*;

public class Features {

    private Map<String, String> parameters;

    public Features(Map<String, String> parameters){
        this.parameters = parameters;
    }

    public double [] getDocFeatures(String[] terms, String externalDocid) throws Exception {
        int internalID = Idx.getInternalDocid(externalDocid);

        double [] features = new double[18];

        features[0] = Double.parseDouble(Idx.getAttribute("spamScore", internalID));

        String rawUrl = Idx.getAttribute ("rawUrl",internalID);
        rawUrl = rawUrl.replace("http://", "");
        rawUrl = rawUrl.replace("https://", "");

        features[1] = rawUrl.length() - rawUrl.replace("/","").length();

        if(rawUrl.contains("wikipedia.org"))
            features[2] = 1;
        else
            features[2] = 0;

        features[3] = Double.parseDouble(Idx.getAttribute ("PageRank", internalID));

        features[4] = RetrievalModelBM25.getScoreBM25(internalID, "body", terms,parameters);

        features[5] = RetrievalModelIndri.getScoreIndri(internalID, "body", terms,parameters);

        features[6] = getOverlapScore(internalID, "body", terms);

        features[7] =  RetrievalModelBM25.getScoreBM25(internalID, "title", terms,parameters);

        features[8] = RetrievalModelIndri.getScoreIndri(internalID, "title", terms,parameters);

        features[9] = getOverlapScore(internalID, "title", terms);

        features[10] =  RetrievalModelBM25.getScoreBM25(internalID, "url", terms,parameters);

        features[11] = RetrievalModelIndri.getScoreIndri(internalID, "url", terms,parameters);

        features[12] = getOverlapScore(internalID, "url", terms);

        features[13] =  RetrievalModelBM25.getScoreBM25(internalID, "inlink", terms,parameters);

        features[14] = RetrievalModelIndri.getScoreIndri(internalID, "inlink", terms,parameters);

        features[15] = getOverlapScore(internalID, "inlink", terms);

        features[16] = getQuerytermDensity(internalID,"body", terms);

        features[17] = getTFIDFscore(internalID, "body", terms);

        return features;

    }

    public double getOverlapScore(int internalID, String field, String[] terms) throws  Exception{

        TermVector vector = new TermVector(internalID, field);

        if(vector.stemsLength()==0){
            return  Double.NaN;
        }

        int match = 0;

        int len = terms.length;

        for(int i=0; i<len; i++){
            if(vector.indexOfStem(terms[i]) != -1){
                match ++;
            }
        }

        return  ((double)match/(double)len);

    }

    public double getQuerytermDensity (int internalID, String field, String[] terms) throws  Exception{
        TermVector vector = new TermVector(internalID, field);

        if(vector.stemsLength()==0){
            return  Double.NaN;
        }

        int count = 0;
        double docLen = (double) Idx.getFieldLength(field,internalID);
        int index = 0;

        for(int i=0; i<terms.length; i++) {
            if (vector.indexOfStem(terms[i]) != -1){
                index = vector.indexOfStem(terms[i]);
                count+=vector.stemFreq(index);
            }
        }

        return ((double) count) / docLen;

    }

    public double getTFIDFscore(int internalID, String field, String[] terms) throws Exception{
        TermVector vector = new TermVector(internalID, field);

        if(vector.stemsLength()==0){
            return  Double.NaN;
        }

        double tf = 0;
        double df = 0;
        double idf =0;
        int index = 0;
        double score = 0;

        for(int i=0; i<terms.length; i++){
            if(vector.indexOfStem(terms[i]) != -1){
                index = vector.indexOfStem(terms[i]);
                tf = (double) vector.stemFreq(index);
                df = (double) vector.stemDf(index);

                idf = Math.max(0,Math.log( (Idx.getNumDocs()-df+0.5)/(df + 0.5) ));

                score += tf*idf;
            }

        }
        return score;

    }

    public void normalizeFeature(LinkedHashMap<String, double[]> docANDvector, String qid, PrintWriter featureWriter){
        double [] featureMax = new double [18];
        double [] featureMin = new double [18];
        Arrays.fill(featureMax,Double.NEGATIVE_INFINITY);
        Arrays.fill(featureMin,Double.POSITIVE_INFINITY);


        for(int i=0; i<featureMax.length;i++){
            for(String key : docANDvector.keySet()){
                double [] feature = docANDvector.get(key);
                if(Double.isNaN(feature[i]))
                    continue;
                if(feature[i]>featureMax[i])
                    featureMax[i] = feature[i];
                if(feature[i]<featureMin[i])
                    featureMin[i] = feature[i];
            }
        }

        for(String key : docANDvector.keySet()){
            String [] disable =null;
            double  [] feature = docANDvector.get(key);
            if(parameters.containsKey("letor:featureDisable")){
                disable = parameters.get("letor:featureDisable").split(",");
            }

            String relevance = (Integer.parseInt(key.split(" ")[1]) + 3) + "";
            featureWriter.print(relevance + " qid:" + qid + " ");
            for(int i=0; i<feature.length; i++){
                if(disable!=null && hasDisable(disable,i)){
                    continue;
                }

//                if(Double.isNaN(feature[i])){
//                    System.out.println("+++");
//                    feature[i] = 0;
//                }


                if( !(Double.isNaN(feature[i])) ){
                    if(!(featureMax[i]==featureMin[i])){
                        feature[i] = (feature[i] - featureMin[i]) / (featureMax[i]-featureMin[i]);
//                        if(Double.isNaN(feature[i])){
//                            System.out.println("featureMax[i] :"+featureMax[i]);
//                            System.out.println("featureMin[i] :"+featureMin[i]);
//                        }
                        //feature[i] += 0.5;
                    }else {
                        feature[i] = 0;
                    }
                }else{
                    feature[i] = 0;
                }



                featureWriter.print((i + 1) + ":" + feature[i] + " ");
            }

            featureWriter.print("# " + key.split(" ")[0] + "\n");
        }

    }

    //public void writeFeature(){}


    public boolean hasDisable(String str[],int n){
        n++;
        String strn = ""+n;
        for(String s: str){
            if(s.equals(strn))
                return true;
        }
        return false;
    }

}
