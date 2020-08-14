import java.util.*;

public class DocWithDiversity {
    private List<Double> diversityScoreList;
    private double initScore;
    private String eid;

    DocWithDiversity(String eid, double initScore){
        this.diversityScoreList = new ArrayList<>();
        this.eid = eid;
        this.initScore = initScore;
    }

    public void setInitScore(double score){
        this.initScore = score;
    }

    public List<Double> getDiversityScoreList(){
        return this.diversityScoreList;
    }

    public void incrementDiversityScoreList(double score){
        this.diversityScoreList.add(score);
    }


    public double getInitScore(){
        return this.initScore;
    }

    public String getExternalID(){
        return this.eid;
    }

    public void normList(double max){
        for(int i=0; i<this.diversityScoreList.size();i++)
            diversityScoreList.set(i,diversityScoreList.get(i)/max);

    }

    public void normScore(double max){
        this.initScore = this.initScore/max;
    }
}
