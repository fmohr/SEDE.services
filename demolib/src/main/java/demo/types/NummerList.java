package demo.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NummerList extends ArrayList<Double> implements List<Double>{

	private static final long serialVersionUID = 1L;
	public NummerList() {

	}
	public NummerList(List<Number> list) {
		for(Number n : list) {
			this.add(n.doubleValue());
		}
	}
	public NummerList(Number... numbers) {
		this(Arrays.asList(numbers));
	}
}
