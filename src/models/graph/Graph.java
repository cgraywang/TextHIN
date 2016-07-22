package models.graph;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;



public class Graph
{
/*	protected final int MAX_VERTS = 20;
*/	protected Vertex vertexList[]; // vertex array
	public DoubleMatrix2D adjMat = null; // adjacency matrix
	protected int nVerts; // vertex number

	public Graph(int vertexNum) 
	{
		nVerts = vertexNum;
		vertexList = new Vertex[vertexNum];
		for (int i = 0; i < vertexNum; ++i) {
			vertexList[i] = new Vertex(i+"");
		}
		adjMat = new DenseDoubleMatrix2D(vertexNum, vertexNum);
	} 

	public void addVertexName(int index, String lab)
	{
		vertexList[index].setVertexName(lab);
	}

	public void setEdge(int start, int end)
	{
		adjMat.setQuick(start, end, 1);
		adjMat.setQuick(end, start, 1);
	}
	
	public void trimAdjMatToSize() {
		adjMat.trimToSize();
	}

	public void displayVertex(int v)
	{
		System.out.print(vertexList[v].label);
	}

	// returns an unvisited vertex adjacent to v
	public int getAdjUnvisitedVertex(int v)
	{
		for (int j = 0; j < nVerts; ++j)
			if (adjMat.getQuick(v, j) == 1 && vertexList[j].wasVisited == false)
				return j;
		return -1;
	}  
	   
} 
