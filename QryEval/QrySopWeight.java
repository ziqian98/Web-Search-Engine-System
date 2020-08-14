import java.util.*;
import java.io.*;

public abstract  class QrySopWeight extends QrySop {
    private List<Double> weightList = new ArrayList<>();

    public void addWeight(double weight){
        weightList.add(weight);
    }

    public double getWeight(int i){
        return weightList.get(i);
    }

    public double getSumWeight(){
        double sum = 0.0;
        for(int i=0; i<weightList.size();i++)
            sum = sum +weightList.get(i);
        return sum;
    }

    public  abstract double getScore(RetrievalModel r) throws IOException;

    public  abstract double getDefaultScore(RetrievalModel r, long docid) throws IOException;

    public void initialize(RetrievalModel r) throws IOException {
        for (Qry q_i: this.args) {
            q_i.initialize (r);
        }
    }

}



