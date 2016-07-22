package models.graph;


public class VertexMSTW extends Vertex
{
	public boolean isInTree;

	public VertexMSTW(String lab)   // constructor
	{
		label = lab;
		isInTree = false;
	}
	public VertexMSTW() // constructor
	{
		this("unindex node");
	}

}  // end class Vertex