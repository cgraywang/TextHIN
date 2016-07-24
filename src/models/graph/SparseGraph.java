package models.graph;

import java.util.ArrayList;
import java.util.List;


/**
 * sparse graph version
 * @author Haoran Li
 */


public class SparseGraph {
	/*	protected final int MAX_VERTS = 20;
	*/	protected Vertex vertexList[]; // vertex array
	
		protected List<List<Edge>> adjMat;
	
//		protected List<AbstractIntDoubleMap> adjMat = null; // adjacency matrix
		protected int nVerts; // vertex number
		protected double nEdges;

		public SparseGraph(int vertexNum) 
		{
			nVerts = vertexNum;
			nEdges = 0;
			vertexList = new Vertex[vertexNum];
			for (int i = 0; i < vertexNum; ++i) {
				vertexList[i] = new Vertex(i+"");
			}
			adjMat = new ArrayList<List<Edge>>(vertexNum);
			for (int i = 0; i < vertexNum; i++)
			{
				adjMat.add(new ArrayList<Edge>());
			}
				
		} 

		public void addVertexName(int index, String lab)
		{
			vertexList[index].setVertexName(lab);
		}

		public void setEdge(int start, int end)
		{
			setEdge(start, end, 1);
		}
		
		public void setEdge(int start, int end, double value)
		{
			adjMat.get(start).add(new Edge(start, end, value));
			adjMat.get(end).add(new Edge(end,  start, value));
			vertexList[start].degree += value;
			vertexList[end].degree += value;
			nEdges += value;
		}

		public void displayVertex(int v)
		{
			System.out.print(vertexList[v].label);
		}

		// returns an unvisited vertex adjacent to v
		public List<Edge> getAdjEdges(int u)
		{
			return adjMat.get(u);
		}  
}
