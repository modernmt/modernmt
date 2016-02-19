package eu.modernmt.test.contextanalyzer.context;

import eu.modernmt.context.ContextDocument;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Created by lucamastrostefano on 17/02/16.
 */
public class Stats {

    protected int numberOfMatches;
    protected int sumPosition;
    protected double sumScoreGap;
    protected int sumRecall;
    protected int numberOfQuery;
    protected long totalTime;

    public Stats(){
        this.numberOfMatches = 0;
        this.sumPosition = 0;
        this.sumScoreGap = 0;
        this.sumRecall = 0;
        this.numberOfQuery = 0;
        this.totalTime = 0;
    }

    public void addSample(List<ContextDocument> results, String domainId, long queryTime){
        this.numberOfQuery++;
        boolean match = results.get(0).getId().equals(domainId);
        boolean found = false;
        int position = -1;
        double gap = -1;
        for(int i = 0; i < results.size(); i++){
            if(results.get(i).getId().equals(domainId)){
                position = (i+1);
                if ((i+1) < results.size()){
                    gap = results.get(i).getScore() - results.get(i+1).getScore();
                } else{
                    gap = results.get(i).getScore();
                }
                found = true;
            }
        }
        this.add(match, position, gap, found, queryTime);
    }

    private synchronized void add(boolean match, int position, double gap, boolean found, long queryTime){
        if(match) {
            this.numberOfMatches++;
        }
        if(found) {
            this.sumRecall++;
            this.sumPosition += position;
            this.sumScoreGap += gap;
        }
        this.totalTime += queryTime;
    }

    @Override
    public String toString(){
        return "Precision: " + this.numberOfMatches /(double)this.numberOfQuery +
                "\tRecall: " + this.sumRecall/(double)this.numberOfQuery +
                "\tAvgPosition: " + this.sumPosition/(double)this.sumRecall +
                "\tAvgScoreGap: " + this.sumScoreGap/(double)this.sumRecall +
                "\tAvgQuertTime " + this.totalTime/(double)this.numberOfQuery;
    }

    public JSONObject getJson(){
        JSONObject jsonStats = new JSONObject();
        jsonStats.put("numberOfMatches", this.numberOfMatches /(double)this.numberOfQuery);
        jsonStats.put("Recall", this.sumRecall/(double)this.numberOfQuery);
        jsonStats.put("avgPosition", this.sumPosition/(double)this.sumRecall);
        jsonStats.put("avgScoreGap", this.sumScoreGap/(double)this.sumRecall);
        jsonStats.put("AvgQuertTime", this.totalTime/(double)this.numberOfQuery);
        return jsonStats;
    }
}
