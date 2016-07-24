package edu.pku.dlib.KnowSim;

import com.google.common.base.Joiner;

/**
 * class for a kind of relationships.
 * @author Haoran Li
 */

public class RelationInfo {
	public String[] relations;
	public String argType;
	public String retType;
	
	public RelationInfo(String retType, String argType, String relation) {
		this.retType = retType;
		this.argType = argType;
		this.relations = new String[1];
		this.relations[0] = relation;
	}
	
	public RelationInfo(String retType, String argType, String relation1, String relation2) {
		this.retType = retType;
		this.argType = argType;
		this.relations = new String[2];
		this.relations[0] = relation1;
		this.relations[1] = relation2;
	}
	
	public RelationInfo(String retType, String argType, String[] relations) {
		this.retType = retType;
		this.argType = argType;
		this.relations = new String[relations.length];
		for (int i = 0; i < relations.length; i++) 
			this.relations[i] = relations[i];
	}
	
	public String relationString() {
		return Joiner.on("^").join(relations);
	}
	
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append(retType);
		ret.append(" " + Joiner.on(" ").join(relations) + " ");
		ret.append(argType);
		return ret.toString();
	}
	
	public boolean isSameRelation(String[] that_relations) {
		if (this.relations.length != that_relations.length)
			return false;
		int length = this.relations.length;
		for (int i = 0; i < length; i++)
			if (!this.relations[i].equals(that_relations[i]))
				return false;
		return true;
	}
	

}