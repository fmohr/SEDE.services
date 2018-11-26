package de.upb.sede.services.mls;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.sede.services.mls.util.Options;
import weka.core.Instances;
import weka.filters.Filter;

public class WekaFilterWrapper implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger("MLS");

	private final String filterName;

	private Filter filter;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public WekaFilterWrapper(String filterName) throws Exception {
		logger.debug("Created wrapper for filter '{}'.", filterName);
		this.filterName = filterName;
		// make sure the given classname is a filter:
		if(!Filter.class.isAssignableFrom(Class.forName(filterName))){
			throw new RuntimeException("The specified classname is not a filter: " + filterName);
		} else {
			construct();
		}
	}

	public void set_options_dict(Map options) throws Exception {
		if(filter == null) {
			return;
		}
		if(options == null) {
			return;
		}
		String[] optionArr = Options.flattenMapToArr(options, true);
		this.filter.setOptions(optionArr);
	}

	/**
	 * Constructs a new instance of a classifier.
	 * @throws Exception
	 */
	private void construct() throws Exception {
		Class<?> filterClass = Class.forName(filterName);
		this.filter = (Filter) ConstructorUtils.invokeConstructor(filterClass);
	}

	public void train(Instances instances) throws Exception {
		filter.setInputFormat(instances);
	}

	/**
	 * Applies the useFilter function from Filter using the wrapped filter, delegate.
	 * @param instances Data to be preprocessed.
	 * @return Preprocessed data
	 * @throws Exception throws by useFilter
	 */
	public Instances preprocess(Instances instances) throws Exception {
		Instances filteredInstances = Filter.useFilter(instances, filter);
		return filteredInstances;
	}

	/**
	 * If the filter is a OptionalHandler tries to invoke setOptions on it using the given list of options.
	 * Each string in the given list is split by whitespace
	 * @param options
	 * @throws Exception
	 */
	public void set_options(List options) throws Exception {
		String[] optArr = Options.splitStringIntoArr(options);
		logger.debug("Set option of {} to: {}.", filterName, Arrays.toString(optArr));
		filter.setOptions(optArr);
	}


}
