package de.upb.sede.services.mls.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Options {
	public static String[] splitStringIntoArr(List optionList) {
		if(optionList == null) {
			return new String[0];
		}
		List<String> splittedOptions = new ArrayList<>();
		for(Object opt : optionList) {
			String optStr = opt.toString();
			splittedOptions.addAll(Arrays.asList(optStr.split("\\s")));
		}
		String[] optArr = splittedOptions.toArray(new String[0]);
		return optArr;
	}


	public static String[] flattenMapToArr(Map options, boolean keyKeys) {
		if(options == null) {
			return new String[0];
		}
		List<String> keys = new ArrayList<String>(options.keySet());
		List<String> optionList = new ArrayList<>();

		for (String optionName : keys) {
			String optionVal;
			if (options.get(optionName) instanceof String) {
				optionVal = (String) options.get(optionName);
			} else {
				continue;
			}
			if (keyKeys) {
				optionList.add(optionName);
			}
			optionList.add(optionVal);
		}

		return optionList.toArray(new String[0]);
	}
}
