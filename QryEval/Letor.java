import java.io.*;
import java.util.*;

public class Letor {

    private Map<String, String> parameters;

    public Letor(Map<String, String> parameters){
        this.parameters = parameters;
    }

    public void go() throws Exception{
        TrainingFeatures train = new TrainingFeatures();
        train.produceTrainFeature(getParam());

        TestingFeatures test = new TestingFeatures();
        test.produceTestFeature(getParam());
    }

    public Map<String, String> getParam(){
        return this.parameters;
    }
}
