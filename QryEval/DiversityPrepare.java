import java.util.*;
import java.io.*;

public class DiversityPrepare {
    private Map<String, String> parameters;

    DiversityPrepare(Map<String, String> parameters){
        this.parameters = parameters;
    }

    public List<QueryWithDiversity> getAllDiversityQ() throws IOException {
        List<QueryWithDiversity> queryANDintent = new ArrayList<>();
        BufferedReader queryReader = new BufferedReader(new FileReader(this.parameters.get("queryFilePath")));
        BufferedReader intentReader = new BufferedReader(new FileReader(this.parameters.get("diversity:intentsFile")));

        String eachQuery = null;
        String eachIntent = null;

        boolean firstTime = true;

        while (true) {
            eachQuery = queryReader.readLine();
            if (eachQuery == null)
                break;

            QueryWithDiversity query = new QueryWithDiversity(eachQuery.substring(eachQuery.indexOf(":") + 1),
                    eachQuery.substring(0, eachQuery.indexOf(":")));

            if (firstTime)
                eachIntent = intentReader.readLine();
            firstTime = false;


            while (true) {
                if (eachIntent == null)
                    break;
                if ((eachIntent.split("\\.")[0]).equals(eachQuery.substring(0, eachQuery.indexOf(":")))) {
                    query.incrementDiversityQlist(eachIntent.substring(eachIntent.indexOf(":") + 1));
                    eachIntent = intentReader.readLine();
                } else {
                    break;
                }
            }

            queryANDintent.add(query);
        }

        queryReader.close();
        intentReader.close();
        return queryANDintent;
    }

}

