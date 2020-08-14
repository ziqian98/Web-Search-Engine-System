import java.io.IOException;
import java.util.*;


public class SVM {
    public void train(Map<String, String> parameters) throws Exception {
        Process cmdProc = Runtime.getRuntime().exec(
                                new String[] {  parameters.get("letor:svmRankLearnPath"),
                                "-c",
                                parameters.get("letor:svmRankParamC"),
                                parameters.get("letor:trainingFeatureVectorsFile"),
                                parameters.get("letor:svmRankModelFile") });

        //wait for training process
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }

    }

    public void test(Map<String, String> parameters) throws Exception{
        Process cmdProc = Runtime.getRuntime().exec(
                                new String[] { parameters.get("letor:svmRankClassifyPath"),
                                parameters.get("letor:testingFeatureVectorsFile"),
                                parameters.get("letor:svmRankModelFile"),
                                parameters.get("letor:testingDocumentScores") });


        //wait for testing process
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }

    }

}
