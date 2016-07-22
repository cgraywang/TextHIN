package models.graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;




public class PageRank_Nibble {
	protected SparseGraph graph;
	double[] p;
	double[] r;
	public PageRank_Nibble(int vertexNum) {
		graph = new SparseGraph(vertexNum);
	}
	
	public void setVertex(int idx, String label)
	{
		graph.addVertexName(idx, label);
	}
	
	public void addUndirectedEdge(int st, int ed, double value)
	{
		graph.setEdge(st, ed, value);
	}
	
	public void check(){
		double sum = 0;
		for (int i = 0; i < graph.nVerts; i++)
		{
			sum += p[i];
			sum += r[i];
		}
		if (sum < 0.90)
			System.out.println("error");
	}
	
	public void addVertexToQueue(int u, double eps, Queue<Integer> queue, boolean[] inQueue)
	{
		if (inQueue[u])
			return;
		if (r[u] > graph.vertexList[u].degree * eps)
		{
			queue.add(u);
			inQueue[u] = true;
		}
	}
	
	public void push(int u, double alpha, Queue<Integer> queue, boolean[] inQueue, double eps)
	{
		if (graph.vertexList[u].degree == 0)
			return;
			
		double pro = r[u] * (1 - alpha) * 0.5 ;
		p[u] += alpha * r[u];
		r[u] = pro;
		addVertexToQueue(u, eps, queue, inQueue);
		pro /= graph.vertexList[u].degree;
		for (Edge edge: graph.getAdjEdges(u))
		{

			int v = edge.destVert;

			r[v] += pro * edge.distance;
			addVertexToQueue(v, eps, queue, inQueue);
		}
	}
	
	public double log2(double value)
	{
		return Math.log(value) / Math.log(2);
	}
	
	class IndexDoublePair implements Comparable<IndexDoublePair>
	{
		int vertex;
		double value;
		
		public IndexDoublePair(int vertex, double value, double degree) {
			this.vertex = vertex;
			if (degree == 0)
				this.value = 0;
			else
				this.value = value / degree;
		}
		
		public int compareTo(IndexDoublePair o) 
		{
			if (this.value > o.value)
				return -1;
			else if (this.value < o.value)
				return 1;
			else 
				return 0;
		};
		
	}
	
	public Set<Integer> PRNibble(int[] start_vertexes, double conductance, int b, Map<String, Double> setting)
	{
		double m = graph.nEdges + 0.0;
		
		double B = Math.ceil(log2(m));
		double alpha = conductance * conductance / (225 * log2(100 * Math.sqrt(m)));
		double eps = Math.pow(2, -b) * ( 1.0 / (48 * B));
		if (setting != null && setting.containsKey("alpha"))
			alpha = setting.get("alpha");
		if (setting != null && setting.containsKey("eps"))
			eps = setting.get("eps");
		
//		alpha = 0.3;
//		eps = 0.12;
		
		/*
		System.out.println("alpha:"+alpha);
		System.out.println("eps:"+eps);
		System.out.println("B:"+B);
		System.out.println("m:" + m +" log2m:"+log2(m));
		System.out.println("n:"+graph.nVerts);
		*/
		
		approximatePageRank(start_vertexes, alpha, eps);
		IndexDoublePair[] ppr_score = new IndexDoublePair[graph.nVerts];
		for (int i = 0; i < graph.nVerts; i++)
			ppr_score[i] = new IndexDoublePair(i, p[i], graph.vertexList[i].degree);
		Arrays.sort(ppr_score);
				
				
		double connect_size = 0;
		double cluster_size = 0;
		/*
		int visited_cnt = 0;
		for (int i = 0;  i < graph.nVerts; i++)
		{
			
			if (ppr_score[i].value > 0.00000001)
				visited_cnt += 1;
		}
		System.out.println("have visited:" + visited_cnt);
		*/
		
		Set<Integer> cluster_set = new HashSet<Integer>();
		double tot_size = graph.nEdges * 2;
		double oo = 1e10;
		double min_size = 0;
		double min_conductance = oo;
		for (int i = 0; i < graph.nVerts; i++)
		{
			int vertex = ppr_score[i].vertex;
			if (p[vertex] < 0.0000000001)
				break;
			cluster_set.add(vertex);
			cluster_size += graph.vertexList[vertex].degree;
			for (Edge edge: graph.getAdjEdges(vertex))
			{
				int v = edge.destVert;
				if (v == vertex)
					continue;
				if (cluster_set.contains(v))
					connect_size -= edge.distance;
				else 
					connect_size += edge.distance;
			}
			double now_conductance;
			if (Math.min(cluster_size, tot_size-cluster_size) < 0.0001)
				now_conductance = 0;
			else 
				now_conductance = (connect_size + 0.0) / Math.min(cluster_size, tot_size-cluster_size);
			if (now_conductance < min_conductance || i < 3000)
			{
				min_conductance = now_conductance;
				min_size = i + 1;
//				System.out.println("subgraph_size:" + i + "\tconnect:" + connect_size + "\tcluster_size:" + Math.min(cluster_size, tot_size-cluster_size));
			}
		}
		
//		for (int i = 0; i < graph.nVerts; i++)
//			System.out.print(r[i] + "\t");
//		System.out.println();
//		System.out.println("cluster_size:" + min_size);
//		System.out.println("conductance:" + min_conductance);
//		if (min_size == 0 || min_conductance > conductance)
//			return null;
		Set<Integer> cluster = new HashSet<Integer>();
		for (int i = 0; i < min_size; i++)
			cluster.add(ppr_score[i].vertex);
		return cluster;
	}
	
	public void approximatePageRank(int[] start_vertexes, double alpha, double eps)
	{
		Queue<Integer> queue = new LinkedList<Integer>();
		p = new double[graph.nVerts];
		r = new double[graph.nVerts];
		boolean[] inQueue = new boolean[graph.nVerts];
//		r[stv] = 1;
		/* even distribution 
		for (int i = 0; i < start_vertexes.length; i++)
		{
			int vertex = start_vertexes[i];
			r[vertex] = 1.0 / start_vertexes.length;
			queue.add(vertex);
			inQueue[vertex] = true;
			
		}
		*/
		double tot_degree = 0;
		for (int i = 0; i < start_vertexes.length; i++)
			tot_degree += graph.vertexList[start_vertexes[i]].degree;
		for (int i = 0; i < start_vertexes.length; i++)
		{
			int vertex = start_vertexes[i];
			r[vertex] = 1.0 / tot_degree * graph.vertexList[vertex].degree;
			queue.add(vertex);
			inQueue[vertex] = true;
		}
		Integer u;
		while ((u = queue.poll()) != null)
		{
			inQueue[u] = false;
			push(u, alpha, queue, inQueue,eps);
//			check();
		}
		check();
	}
	
	public static void main(String[] args) {
		double[][] adj = {
				{0,1,0,0},
				{1,0,1,0},
				{0,1,0,1},
				{0,0,1,0}
		};
		PageRank_Nibble nibble = new PageRank_Nibble(4);
		for (int i = 0; i < 4; i++)
			for (int j = i+1; j < 4; j++)
				if (adj[i][j] != 0)
					nibble.addUndirectedEdge(i,j,adj[i][j]);
		int[] starts = {0,2};
		System.out.println(nibble.PRNibble(starts, 0.5, 1,null));
	}
}
