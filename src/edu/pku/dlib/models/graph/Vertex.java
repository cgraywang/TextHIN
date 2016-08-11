package edu.pku.dlib.models.graph;

/**
 * @author Haoran Li
 */


public class Vertex
{
	public String label; // label (e.g. "A")
	public boolean wasVisited;
	double degree;
	public Vertex (String lab) // constructor
	{
		degree = 0;
		label = lab;
		wasVisited = false;
	}
	public Vertex () // constructor
	{
		this("unindex node");
	}
	public void setVertexName (String lab) {
		label = lab;
	}
}
