package models.util;

import java.util.logging.Logger;

// Maintain a minheap to find k-nearest neighbors.
public class MinHeap{
	private int[] indices = null;
	private double[] values = null;
	private int size = 0;
	private int capacity = 8;
	
	private static Logger logger = Logger.getLogger("basic.util");	

	public MinHeap(int[] indices, double[] values, int cap) {
		if (indices.length != values.length) {
			logger.warning("Dimensions does not match!");
		}
		this.indices = indices.clone();
		this.values = values.clone();
		size = indices.length;
		capacity = cap;
	}
	public MinHeap(int cap) {
		this.indices = new int[cap];
		this.values = new double[cap];
		capacity = cap;
		size = 0;
	}
	
	public int heapSize() { 
		return size;
	}
	public boolean isLeaf(int pos) { 
		return (pos >= (size/2)) && (pos < size);
	}
	public int leftChild(int pos) {
		if (pos > size/2) {
			logger.warning("Position has no left child");
		}
		return 2 * pos + 1;            
	}
	public int rightChild(int pos) {
		if (pos > (size - 1)/2) {
			logger.warning("Position has no right child");
		}
		return 2 * pos + 2;
	}
	public int parent(int pos) { // Return position for parent
		if (pos < 0) {
			logger.warning("Position has no parent");
		}
		return ((pos-1)/2);
	}
	public void swap(int index1, int index2) {
		int index = indices[index1];
		double value = values[index1];
		indices[index1] = indices[index2];
		values[index1] = values[index2];
		indices[index2] = index;
		values[index2] = value;
	}
	public void insert(int index, double value) {
		if (size >= capacity) {
			logger.warning("Get heap max capacity!");
			return;
		}	  
		int curr = size++;
		if (size > indices.length) {
			int[] newindices = new int[size];
			double[] newvalues = new double[size];
			for (int i = 0; i < size - 1; ++i) {
				newindices[i] = indices[i];
				newvalues[i] = values[i];
			}
			indices = newindices;
			values = newvalues;
		}
		indices[size-1] = index;        // Start at end of MinHeap
		values[size-1] = value;
		
		while (curr != 0 && values[curr] < values[parent(curr)])
		{
			swap(curr, parent(curr));
			curr = parent(curr);
		}
	}

	public void buildheap() {
		for (int i = (size/2) - 1; i >= 0; --i) {
			heapify(i); 
		}
	}

	private void heapify(int pos) {
		if ((pos < 0) && (pos >= size)) {
			logger.warning("Illegal heap position");
		}
		while (!isLeaf(pos)) {
			int j = leftChild(pos);
			if ((j<(size-1)) && (values[j] < values[j + 1])) {
				j++; // j is now index of child with greater value
			}
			if (values[pos] <= values[j]) 
				return; // Done
			swap(pos, j);
			pos = j;  // Move down
		}
	}

	public void changeMin(int index, double value) {
		if (size < 0) {
			logger.warning("Changing: Empty heap");
		}
		indices[0] = index;
		values[0] = value;
		heapify(0);   // Put new heap root val in correct place
	}
	
	public int removeMin() {
		if (size < 0) {
			logger.warning("Removing: Empty heap");
		}
		--size;
		swap(0, size); // Swap maximum with last value
		if (size != 0)      // Not on last element
			heapify(0);   // Put new heap root val in correct place
		return indices[size];
	}

	public double min() {
		return values[0];
	}
	
	public int[] getIndices() {
		return indices;
	}
	
	public double[] getValues() {
		return values;
	}

	public static int[] heapSort(MinHeap minHeap) {
		int[] newIndices = new int[minHeap.getIndices().length];
		int orgsize = minHeap.size;
		for (int i = 0; i < orgsize; ++i) { // Now sort
			newIndices[i] = minHeap.removeMin();   // removeMax places max value at end of heap
		}
		return newIndices;
	}

}
