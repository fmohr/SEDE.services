package de.upb.sede.services.mls;

import de.upb.sede.services.mls.util.Options;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WekaBClustererWrapper implements Serializable, DictOptionsHandler, ListOptionsHandler {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger("MLS");

	private final String clustererName;
	private Clusterer clusterer;

	/**
	 * Default constructor of the wrapper.
	 */
	public WekaBClustererWrapper(String clustererName) throws Exception {
			logger.trace("Wrapper for clusterer '{}' created.", clustererName);
			this.clustererName = clustererName;
			// make sure the given classname is a clustererName:
			if(! Clusterer.class.isAssignableFrom(Class.forName(clustererName))){
				throw new RuntimeException("The specified classname is not a clustererName: " + clustererName);
			}
			construct();
		}

		/**
		 * Constructs a new instance of a clusterer.
		 * @throws Exception
		 */
		private void construct() throws Exception {
			Class<?> classifierClass = Class.forName(clustererName);
			clusterer = (Clusterer) ConstructorUtils.invokeConstructor(classifierClass);
		}

		/**
		 *	Builds the clusterer using the given data.
		 * @param data training data.
		 * @throws Exception exception thrown during invocation of 'buildClusterer'.
		 */
		public void build(Instances data) throws Exception {
			/* Recreate the classifier object. */
			clusterer.buildClusterer(data);
		}

		/**
		 * Uses the built clusterer to do clustering on the given data.
		 * @param data input data
		 * @return predicted cluster index
		 * @throws Exception throw during invocation of 'classifyInstance'
		 */
		public List<Integer> cluster(Instances data) throws Exception {
			if(clusterer== null) {
				throw new RuntimeException("First fit the model.");
			}
			List<Integer> clusters = new ArrayList<>(data.size());
			for(Instance instance : data) {
				Double prediction;
				int clusterId = clusterer.clusterInstance(instance);
				clusters.add(clusterId);
			}
			return clusters;
		}

		/**
		 * If the clusterer is a OptionalHandler tries to invoke setOptions on it using the given list of options.
		 * @param options
		 * @throws Exception
		 */
		public void set_options(List options) throws Exception {
			if(clusterer instanceof OptionHandler) {
				String[] optArr = Options.splitStringIntoArr(options);
				logger.trace("Set option of {} to: {}.", clustererName, Arrays.toString(optArr));
				((OptionHandler) clusterer).setOptions(optArr);
			}
			else {
				logger.error("Cannot set the options of {}. Options to be set:\n{}", clustererName, options.toString());
			}
		}

		@Override
		public void set_options_dict(Map options) throws Exception {
			this.set_options(Arrays.asList(Options.flattenMapToArr(options, true)));
		}
}
