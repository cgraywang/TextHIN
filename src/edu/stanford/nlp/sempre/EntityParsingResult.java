package edu.stanford.nlp.sempre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.sempre.cache.PopularityCache;
import edu.stanford.nlp.sempre.cache.PopularityCache.Options;
import edu.stanford.nlp.sempre.freebase.EntityLexicon;
import edu.stanford.nlp.sempre.freebase.lexicons.LexicalEntry;
import fig.basic.Option;

/**
 * @author Haoran Li
 */

public class EntityParsingResult {

    public static class Options {
    	// for 20NG and GCAT
    	@Option(gloss = "whether to adjust location score")
    	boolean localtionAdjust = true;
    	@Option(gloss = "wheher to adjust commmon.topic score")
    	boolean commonTopicAdjust = false;
    	// for 20NG
    	@Option(gloss = "whether to adjust computer score")
    	boolean computerAdjust = true;
    	@Option(gloss = "whether to adjust relation of location domain")
    	boolean locationRelationAdjust = true;
    	// for GCAT
    	@Option(gloss = "whether to adjust sports.location score")
    	boolean sportsLocationAdjust = false;
    	
	}
	public static Options opts = new Options();
	public int st;
	public int ed;
	public String phrase;
	public String entity;
	public String type;
	double alignmentScore;
	double typePop = 0;
	double domainPop = 0;
	double popScore = 0;
	double weakRelation = 0;
	double strongRelation = 0;
	double typeRelation = 0;
	double hideRelation = 0;
	double score = 0;
	public boolean adjusted = false;
	static Set<String> lowerSportsDomain;
	public List<Relation> relations;
	
	public EntityParsingResult(int st, int ed, String phrase, String entity, String type, double score) {
		
		this.st = st;
		this.ed = ed;
		this.phrase = phrase;
		this.entity = entity;
		this.type = type;
		this.alignmentScore = score;
		this.relations = new ArrayList<>();
		init();
	}
	
	public void init() {
		if (lowerSportsDomain == null) {
			lowerSportsDomain = new HashSet<>();
			lowerSportsDomain.addAll(Arrays.asList(PopularityCache.opts.GCATSportsDomains));
			lowerSportsDomain.add("sports");
		}
 
	}
	
	public EntityParsingResult() {
		this.type = "relations";
		this.relations = new ArrayList<>();
	}
	
	public void setPopularity(double domainPop, double typePop) {
		this.domainPop = domainPop;
		this.typePop = typePop;
		this.popScore = Math.log(2 * domainPop + typePop + 1);
	}
	
	public void calcScore() {
		if (! adjusted) {
			if (opts.localtionAdjust && EntityLexicon.getDomain(this.type).equals("location")) {
				popScore = Math.max(popScore - 2, 0);
				alignmentScore = Math.min(alignmentScore, 2);
				this.typeRelation = 0;
			}
			if (opts.computerAdjust && EntityLexicon.getDomain(this.type).equals("computer")) {
				popScore = Math.max(popScore + 2, popScore);
			}
			if (opts.sportsLocationAdjust) {
				String domain = EntityLexicon.getDomain(this.type);
				if (lowerSportsDomain.contains(domain)&& (type.contains("country") || type.contains("location")))
					popScore = 0.01;
			}
			if (opts.commonTopicAdjust && this.type.equals("common.topic")) {
				popScore = 0;
			}
			adjusted = true;
		}
		score = popScore + alignmentScore + typeRelation * 1.5 + Double.min(2.0, hideRelation);
	}
	
	public void addRelation(Relation relation) {
		if (opts.locationRelationAdjust && EntityLexicon.getDomain(this.type).equals("location"))
			return;
		this.relations.add(relation);
		switch(relation.level) {
		case 1:
			this.strongRelation += 1;
			break;
		case 2:
			this.weakRelation += 1;
			break;
		case 3:
			this.hideRelation += 1;
			break;
		}
	}
	
	public String compactString() {
		StringBuffer ret = new StringBuffer();
		ret.append(entity);
		ret.append(" " + type);
		ret.append(" " + phrase);
		return ret.toString();
	}
	
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append(entity);
		ret.append(" " + type);
		ret.append(" " + phrase);
		if (strongRelation > 0)
			ret.append(" strongRelation=" + strongRelation);
		if (weakRelation > 0) 
			ret.append(" weakRelation=" + weakRelation);
		if (hideRelation > 0)
			ret.append(" hideRealtion=" + hideRelation);
		if (typeRelation > 0)
			ret.append(" typeRelation=" + typeRelation);
		ret.append(" score=" + score);
		ret.append(" popScore=" + popScore);
		ret.append(" alignScore=" + alignmentScore);
		return ret.toString();
	}
	public static class EntityParsingResultComparator implements Comparator<EntityParsingResult> {
	    @Override
	    public int compare(EntityParsingResult arg0, EntityParsingResult arg1) {
	    	if (arg0.strongRelation > arg1.strongRelation)
	    		return -1;
	    	if (arg0.strongRelation < arg1.strongRelation)
	    		return 1;
	    	if (arg0.weakRelation > arg1.weakRelation)
	    		return -1;
	    	if (arg0.weakRelation < arg1.weakRelation)
	    		return 1;
//	    	if (arg0.hideRelation > arg1.hideRelation)
//	    		return -1;
//	    	if (arg0.hideRelation < arg1.hideRelation)
//	    		return 1;
	    	if (arg0.typeRelation > arg1.typeRelation)
	    		return -1;
	    	if (arg0.typeRelation < arg1.typeRelation)
	    		return 1;
	    	if (arg0.score > arg1.score)
	    		return -1;
	    	if (arg0.score < arg1.score)
	    		return 1;
	    	return 0;
//	    	if (arg0.popScore > arg1.popScore)
//	    		return -1;
//	    	if (arg0.popScore < arg1.popScore)
//	    		return 1;
//	    	if (arg0.alignmentScore > arg1.alignmentScore)
//	    		return -1;
//	    	if (arg0.alignmentScore < arg1.alignmentScore)
//	    		return 1;
//	    	return 0;
	    }
	  }


	
	
	
}
