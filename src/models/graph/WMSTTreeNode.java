package models.graph;

import java.util.ArrayList;

public class WMSTTreeNode {
	int parent;
	ArrayList<Integer> children;
	ArrayList<Double> distances;

	WMSTTreeNode() {
		parent = -1;
		children = new ArrayList<Integer>();
		distances = new ArrayList<Double>();
	}
	
	public void setParent (int index) {
		parent = index;
	}
	
	public void addChild (int index, double dist) {
		children.add(index);
		distances.add(dist);
	}
	
	public int getParent () {
		return parent;
	}
	
	public ArrayList<Integer> getChildren() {
		return children;
	}
	
	public ArrayList<Double> getDistances() {
		return distances;
	}
	
	public void deleteChild (int index) {
		children.remove(index);
		distances.remove(index);
	}
}
