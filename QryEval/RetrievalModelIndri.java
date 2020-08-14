import java.io.IOException;
import java.util.Map;

public class RetrievalModelIndri extends RetrievalModel{
    private double miu;
    private double lambda;

    public RetrievalModelIndri(double miu, double lambda){
        this.miu = miu;
        this.lambda = lambda;
    }

    public double getLambda() {
        return lambda;
    }

    public double getMiu() {
        return miu;
    }

    @Override
    public String defaultQrySopName() {
        return new String("#and");
    }

    public static double getScoreIndri(int internalID, String field,
                                String[] terms, Map<String, String> parameters) throws IOException {

        TermVector vector = new TermVector(internalID, field);

        if (vector.stemsLength() == 0){
            return Double.NaN;
        }

        double score = 1.0;

        int count = 0;

        for (String term : terms){

            int tf = 0;

            count++;

            if (vector.indexOfStem(term) != -1){
                tf = vector.stemFreq(vector.indexOfStem(term));
                count--;
            }

            double collecLen = Idx.getSumOfFieldLengths(field);

            double ctf = Idx.getTotalTermFreq(field, term);

            double pMLE = ctf / collecLen;

            double mu = Double.parseDouble(parameters.get("Indri:mu"));

            double lambda = Double.parseDouble(parameters.get("Indri:lambda"));

            double docLen = Idx.getFieldLength(field,internalID);

            double p = (1-lambda)*((tf+mu*pMLE)/(docLen+mu))+lambda*pMLE;

            score *= p;

        }
        if (count == terms.length){
            return 0;
        }

        score =  Math.pow(score, 1/(double)terms.length);

        return score;

    }




}
