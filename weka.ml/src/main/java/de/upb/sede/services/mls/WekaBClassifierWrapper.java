package de.upb.sede.services.mls;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.sede.services.mls.util.Options;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;

/**
 * Wrapper for Weka-base classifier.
 */
public class WekaBClassifierWrapper implements Serializable, DictOptionsHandler, ListOptionsHandler {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger("MLS");

	private final String classifierName;
	private Classifier classifier;

	/**
	 * Default constructor of the wrapper.
	 */
	public WekaBClassifierWrapper(String classifierName) throws Exception {
		logger.trace("Wrapper for classifier '{}' created.", classifierName);
		this.classifierName = classifierName;
		// make sure the given classname is a classifier:
		if(!Classifier.class.isAssignableFrom(Class.forName(classifierName))){
			throw new RuntimeException("The specified classname is not a classifier: " + classifierName);
		}
		construct();
	}

	/**
	 * Constructs a new instance of a classifier.
	 * @throws Exception
	 */
	private void construct() throws Exception {
		Class<?> classifierClass = Class.forName(classifierName);
		classifier = (Classifier) ConstructorUtils.invokeConstructor(classifierClass);
	}

	/**
	 *	Fits the model of the classifier using the given data.
	 * @param data training data.
	 * @throws Exception exception thrown during invocation of 'buildClassifier'.
	 */
	public void train(Instances data) throws Exception {
		/* Recreate the classifier object. */
		classifier.buildClassifier(data);
	}

	/**
	 * Uses the trained model to do prediction based on the given data.
	 * @param data input data
	 * @param useDistribution if true, it will use the distributionForInstance method of the classifier.
	 * @return predicted class index
	 * @throws Exception throw during invocation of 'classifyInstance'
	 */
	public List<Double> predict(boolean useDistribution, Instances data) throws Exception {
		if(classifier== null) {
			throw new RuntimeException("First fit the model.");
		}
		List<Double> predictions = new ArrayList<>(data.size());
		for(Instance instance : data) {
			Double prediction;
			if(useDistribution) {
				double[] distribution = classifier.distributionForInstance(instance);
				/*
					Calculate index with maximum distribution:
				 */
				double maximumDis = distribution[0];
				prediction = 0.;
				for (int i = 1; i < distribution.length; i++) {
					if(distribution[i] > maximumDis) {
						maximumDis = distribution[i];
						prediction = Double.valueOf(i);
					}
				}
			} else{
				prediction = classifier.classifyInstance(instance);
			}
			predictions.add(prediction);
		}
		return predictions;
	}

	/**
	 * If the classifier is a OptionalHandler tries to invoke setOptions on it using the given list of options.
	 * Each string in the given list is split by whitespace
	 * @param options
	 * @throws Exception
	 */
	public void set_options(List options) throws Exception {
		if(classifier instanceof OptionHandler) {
			String[] optArr = Options.splitStringIntoArr(options);
			logger.trace("Set option of {} to: {}.", classifierName, Arrays.toString(optArr));
			((OptionHandler) classifier).setOptions(optArr);
		}
		 else {
			logger.error("Cannot set options of {}. Options to be set:\n{}", classifierName, options.toString());
		}
	}

	@Override
	public void set_options_dict(Map options) throws Exception {
		this.set_options(Arrays.asList(Options.flattenMapToArr(options, true)));
	}
}
