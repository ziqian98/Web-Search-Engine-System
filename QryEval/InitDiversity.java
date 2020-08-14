import java.util.*;

public class InitDiversity {
    private Map<String, String> parameters;

    InitDiversity(Map<String, String> parameters){
        this.parameters = parameters;
    }

    public List<DocWithDiversity> getDiversity(QueryWithDiversity eachQuery ) throws  Exception{

        ScoreList queryScoreList = QryEval.processQuery(this.parameters,"doNotCare", eachQuery.getQ(),
                QryEval.initializeRetrievalModel (this.parameters));

        queryScoreList.sort();
        queryScoreList.truncate(Integer.parseInt(this.parameters.get("diversity:maxInputRankingsLength")));

        List<DocWithDiversity> result = new ArrayList<>();

        for(int i=0; i<queryScoreList.size(); i++){
            result.add( new DocWithDiversity(Idx.getExternalDocid(queryScoreList.getDocid(i)),
                    queryScoreList.getDocidScore(i)) );
        }

        for(String eachIntentQuery : eachQuery.getDiversityQlist()){
            ScoreList intentScoreList = QryEval.processQuery(this.parameters,"doNotCare", eachIntentQuery,
                    QryEval.initializeRetrievalModel (this.parameters));

            intentScoreList.sort();
            intentScoreList.truncate(Integer.parseInt(this.parameters.get("diversity:maxInputRankingsLength")));


            Map<String, Double> exidANDscore = new HashMap<>();

            for(int i=0; i<intentScoreList.size(); i++)
                exidANDscore.put(Idx.getExternalDocid(intentScoreList.getDocid(i)), intentScoreList.getDocidScore(i));


            for(DocWithDiversity eachDoc : result) {
                if( ( exidANDscore.get(eachDoc.getExternalID()) )==null)
                    eachDoc.incrementDiversityScoreList(0.0);
                else
                    eachDoc.incrementDiversityScoreList(exidANDscore.get(eachDoc.getExternalID()));


            }

        }

        return result;
    }
}
