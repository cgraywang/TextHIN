package edu.stanford.nlp.sempre;

import java.util.Comparator;

/**
 * @author Haoran Li
 */

public class Relation {
	RelationInfo relation;
	String subj, obj;
	/*
	 * 1: strong relation
	 * 2: weak relation
	 * 3: hide relation
	 */
	int level;
	
	
	public Relation(RelationInfo info, String subj, String obj, int level) {
		this.relation = info;
		this.subj = subj;
		this.obj = obj;
		this.level = level;
	}
	
	public Relation(RelationInfo info, int level) {
		this.relation = info;
		this.level = level;
	}
	
	public boolean isRealRelation() {
		return this.level <= 2;
	}
	
	@Override
	public String toString() {
		return subj + "\t" + relation.relationString() + "\t" + obj + "\t" + "level=" + level;
	}
	
	public static class RelationComparator implements Comparator<Relation> {
		@Override
	    public int compare(Relation arg0, Relation arg1) {
			if (arg0.level < arg1.level)
				return -1;
			if (arg0.level > arg1.level)
				return 1;
			return 0;	
		}
	}
}
