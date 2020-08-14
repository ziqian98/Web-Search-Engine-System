/**
 *  An object that stores parameters for the RANKED BOOLEAN
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */

public class RetrievalModelRankedBoolean extends RetrievalModel {

    public String defaultQrySopName () {
        return new String ("#or");

    }
}
