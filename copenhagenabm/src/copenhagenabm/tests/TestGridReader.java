package copenhagenabm.tests;

import java.io.IOException;

import org.geotools.data.DataSourceException;
import org.geotools.geometry.DirectPosition2D;

import copenhagenabm.tools.RasterSpace;

public class TestGridReader {
	
	
	private RasterSpace rs;

	public TestGridReader() throws IllegalArgumentException, IOException {
		
		// System.out.println(GridFormatFinder.getAvailableFormats());
		
		
		String goodfileName = "geodata/testset/good.asc";
		
		rs = new RasterSpace(goodfileName);
		
		
	}
	
	public void test() {
		System.out.println(rs.valueAt(new DirectPosition2D(12.442, 55.691)));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TestGridReader t = null;
		try {
			t = new TestGridReader();
		} catch (DataSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		t.test();

	}



}
