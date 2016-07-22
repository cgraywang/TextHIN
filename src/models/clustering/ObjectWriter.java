package models.clustering;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
/**
 * @author Haoran Li
 */

public class ObjectWriter {
	public static void writeObject(Object obj, String file)
	{	
		try {
			ObjectOutputStream oout = new ObjectOutputStream(new FileOutputStream(file));
			oout.writeObject(obj);
			oout.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("readObject error");
		} 
	}
	
	@SuppressWarnings("resource")
	public static Object readObject(String file) 
	{
		ObjectInputStream oin;
		try {
			oin = new ObjectInputStream(new FileInputStream(file));
			Object obj = oin.readObject();
			return obj;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("readObject error");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("readObject error");
		}
		
		
	}

	/*
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		List<DoubleMatrix1D> mat = new ArrayList<DoubleMatrix1D>(), new_mat;
		double[] values = {1,2,3,4,5,6,7,8,9,10,0,0,0,1,1,3}, v2 = {1,2,3,4,5};
		DoubleMatrix1D tmp1 = new ColtSparseVector(values);
		DoubleMatrix1D tmp2 = new ColtSparseVector(v2);
		mat.add(tmp1);
		mat.add(tmp2);
		String file = "./test/test.out";
		writeObject(mat, file);
		new_mat = (List<DoubleMatrix1D>)readObject(file);
		System.out.println(new_mat.get(1));
	}
	*/
	
	
}
