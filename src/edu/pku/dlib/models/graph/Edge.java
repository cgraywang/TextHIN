package edu.pku.dlib.models.graph;

/**
 * @author Haoran Li
 */


public class Edge implements Comparable<Edge>
{
	public int srcVert;   // start vertex
	public int destVert;  // end vertex
	public double distance;  // distance or weight

	public Edge(int sv, int dv, double d) 
	{
		srcVert = sv;
		destVert = dv;
		distance = d;
	}

	public int compareTo(Edge arg0) {
		if (distance < arg0.distance)
		    return -1;
		else if (distance == arg0.distance)
		    return 0;
		else return 1;
	}

}  