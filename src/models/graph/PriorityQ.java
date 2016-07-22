package models.graph;

import java.util.ArrayList;

public class PriorityQ
{
	// array in sorted order, from max at 0 to min at size-1
	private ArrayList<Edge> queArray;
	//-------------------------------------------------------------
	public PriorityQ()            // constructor
	{
		queArray = new ArrayList<Edge>();
	}

	public void insert(Edge item)  // insert item in sorted order
	{
		int j;

		for(j=0; j < queArray.size(); j++)           // find place to insert
			if( item.compareTo(queArray.get(j)) > 0 )
				break;
		queArray.add(j, item);

	}

	public Edge removeMin()            // remove minimum item
	{ 
		return queArray.remove(queArray.size() - 1);
	}

	public void removeN(int n)         // remove item at n
	{
		queArray.remove(n);
	}

	public Edge peekMin()          // peek at minimum item
	{ 
		return queArray.get(queArray.size() - 1); 
	}

	public Edge peekN(int n)      // peek at item n
	{ 
		return queArray.get(n); 
	}

	public int size()              // return number of items
	{ 
		return queArray.size(); 
	}

	public boolean isEmpty()      // true if queue is empty
	{ 
		return (queArray.size()==0); 
	}

	public int find(int findDex)  // find item with specified
	{                          // destVert value
		for (int j = 0; j < queArray.size(); j++)
			if (queArray.get(j).destVert == findDex)
				return j;
		return -1;
	}

	public static void main(String[] args)
	{
		PriorityQ pq = new PriorityQ();
		pq.insert(new Edge(0,0,10));
		pq.insert(new Edge(0,0,3));
		pq.insert(new Edge(0,0,2));
		pq.insert(new Edge(0,0,4));
		pq.insert(new Edge(0,0,52));
		pq.insert(new Edge(0,0,32));
		pq.insert(new Edge(0,0,23));
		pq.insert(new Edge(0,0,2531));
		pq.insert(new Edge(0,0,15));
	}
}  // end class PriorityQ