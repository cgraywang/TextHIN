package models.graph;

import java.util.ArrayList;

public class GraphWeightedMST extends Graph
{
	private final double INFINITY = Double.MAX_VALUE;
	private VertexMSTW[] vertexList; 
	private int currentVert;
	private PriorityQ thePQ;
	private int nTree;           // number of vertex in tree
	private ArrayList<WMSTTreeNode> mstTree = new ArrayList<WMSTTreeNode>();

	public GraphWeightedMST(int vertexNum)
	{
		super(vertexNum);
		this.vertexList = new VertexMSTW[vertexNum];
		for (int i = 0; i < vertexNum; ++i) {
			this.vertexList[i] = new VertexMSTW(i+"");
		}
		for (int i = 0; i < vertexNum; ++i) {
			WMSTTreeNode node = new WMSTTreeNode();
			mstTree.add(node);
		}
		for (int i = 0; i < vertexNum; ++i) 
			for (int j = 0; j < vertexNum; ++j)
				adjMat.setQuick(i, j, INFINITY);
		adjMat.trimToSize();
		
		thePQ = new PriorityQ();
	}  

	@Override
	public void addVertexName(int index, String lab)
	{
		this.vertexList[index].setVertexName(lab);
	}

	public void setEdge(int start, int end, double weight)
	{
		adjMat.setQuick(start, end, weight);
	}

	public ArrayList<WMSTTreeNode> getMSTTree() {
		return mstTree;
	}
	
	public void mstw()
	{
		currentVert = 0; 

		
		while(nTree < nVerts-1)// while not all verts in tree
		{                      // put currentVert in tree
			this.vertexList[currentVert].isInTree = true;
			nTree++;

			// insert edges adjacent to currentVert into PQ
			for(int j=0; j<nVerts; j++)   // for each vertex,
			{
				if(j==currentVert)         // skip if it's us
					continue;
				if(this.vertexList[j].isInTree) // skip if in the tree
					continue;
				double distance = adjMat.getQuick(currentVert, j);
				if( distance == INFINITY)  // skip if no edge
					continue;
				putInPQ(j, distance);      // put it in PQ (maybe)
			}
			if(thePQ.size()==0)           // no vertices in PQ?
			{
				System.out.println(" GRAPH NOT CONNECTED");
				return;
			}
			// remove edge with minimum distance, from PQ
			Edge theEdge = thePQ.removeMin();
			int sourceVert = theEdge.srcVert;
			currentVert = theEdge.destVert;
			
			mstTree.get(sourceVert).addChild(currentVert, theEdge.distance);
			mstTree.get(currentVert).setParent(sourceVert);

			// display edge from source to current
			System.out.print( this.vertexList[sourceVert].label + "-> ");
			System.out.print( this.vertexList[currentVert].label + ";");
			System.out.print(" ");
		}  // end while(not all verts in tree)

		// mst is complete
		for(int j=0; j<nVerts; j++)     // unmark vertices
			this.vertexList[j].isInTree = false;
	}  // end mstw

	public void putInPQ(int newVert, double newDist)
	{
		// is there another edge with the same destination vertex?
		int queueIndex = thePQ.find(newVert);
		if(queueIndex != -1)              // got edge's index
		{
			Edge tempEdge = thePQ.peekN(queueIndex);  // get edge
			double oldDist = tempEdge.distance;
			if(oldDist > newDist)          // if new edge shorter,
			{
				thePQ.removeN(queueIndex);  // remove old edge
				Edge theEdge =
					new Edge(currentVert, newVert, newDist);
				thePQ.insert(theEdge);      // insert new edge
			}
			// else no action; just leave the old vertex there
		}  // end if
		else  // no edge with same destination vertex
		{                              // so insert new one
			Edge theEdge = new Edge(currentVert, newVert, newDist);
			thePQ.insert(theEdge);
		}
	}  // end putInPQ()
	public static void main(String[] args)
	{
		GraphWeightedMST theGraph = new GraphWeightedMST(6);
		theGraph.addVertexName(0, "A");     // 0  (start)
		theGraph.addVertexName(1, "B");     // 1
		theGraph.addVertexName(2, "C");     // 2
		theGraph.addVertexName(3, "D");     // 3
		theGraph.addVertexName(4, "E");     // 4
		theGraph.addVertexName(5, "F");     // 5

		theGraph.setEdge(0, 1, 6);  // AB  6
		theGraph.setEdge(0, 3, 4);  // AD  4
		theGraph.setEdge(1, 2, 10); // BC 10
		theGraph.setEdge(1, 3, 7);  // BD  7
		theGraph.setEdge(1, 4, 7);  // BE  7
		theGraph.setEdge(2, 3, 8);  // CD  8
		theGraph.setEdge(2, 4, 5);  // CE  5
		theGraph.setEdge(2, 5, 6);  // CF  6
		theGraph.setEdge(3, 4, 12); // DE 12
		theGraph.setEdge(4, 5, 7);  // EF  7

		System.out.print("Minimum spanning tree: ");
		theGraph.mstw();            // minimum spanning tree
		System.out.println();
	}  // end main()
}  // end class Graph

