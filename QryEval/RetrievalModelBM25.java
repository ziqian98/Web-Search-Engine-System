import java.io.IOException;
import java.util.*;


public class RetrievalModelBM25 extends RetrievalModel {
    private double b;
    private double k1;
    private double k3;


    public RetrievalModelBM25(double b, double k1, double k3){
        this.b = b;
        this.k1 = k1;
        this.k3 = k3;
    }

    @Override
    public String defaultQrySopName() {
        return new String("#sum");
    }

    public double getb() {
        return b;
    }

    public double getk1() {
        return k1;
    }

    public double getk3() {
        return k3;
    }


    public static double getScoreBM25 (int internalID, String field,
                                String[] terms,Map<String, String> parameters) throws IOException {

        TermVector vector = new TermVector(internalID, field);

        double score = 0.0;

        if (vector.stemsLength() == 0){
            return Double.NaN;
        }

        for (String term : terms){

            int index = vector.indexOfStem(term);
            if (index == -1){
                continue;
            }
            float b = Float.parseFloat(parameters.get("BM25:b"));

            float k1 = Float.parseFloat(parameters.get("BM25:k_1"));

            long doclen = Idx.getFieldLength(field, internalID);

            int df = vector.stemDf(index);

            double idfWeight = Math.max(0, Math.log(((double)Idx.getNumDocs()-df + 0.5)/(df + 0.5)) );

            double avgdoclen = (double)Idx.getSumOfFieldLengths(field)/Idx.getDocCount(field);

            int tf = vector.stemFreq(index);

            double tfweight = tf/(tf+k1*( (1-b) + b*(doclen/avgdoclen) ) );

            double userweight = 1.0;

            score += idfWeight * tfweight * userweight;
        }
        return score;
    }





}
