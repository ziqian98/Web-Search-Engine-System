import java.util.*;

public class QueryWithDiversity {
    private List<String> diversityQlist;
    private String q;
    private String qid;

    QueryWithDiversity(String q, String qid){
        this.q = q;
        this.qid = qid;
        this.diversityQlist = new ArrayList<>();
    }

    public String getQ(){
        return this.q;
    }

    public String getQid(){
        return this.qid;
    }

    public List<String> getDiversityQlist(){
        return this.diversityQlist;
    }

    public void incrementDiversityQlist(String intent){
        this.diversityQlist.add(intent);

    }
}
