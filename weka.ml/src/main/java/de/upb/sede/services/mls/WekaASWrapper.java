package de.upb.sede.services.mls;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.sede.services.mls.util.Options;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.OptionHandler;

public class WekaASWrapper implements Serializable, DictOptionsHandler{

	private static final Logger logger = LoggerFactory.getLogger("MLS");

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	
	
	private String asSearcherName, asEvaluatorName;
	private String[] searcherOptions, evalOptions;

	private AttributeSelection attributeSelection;

	private Attribute cachedClassAttribute;
	private Instances cachedInstances;


	public WekaASWrapper() {
		logger.trace("Created wrapper for AS without setting searcher and evaluator.");
	}

	public WekaASWrapper(String asSearcherName, String asEvaluatorName, List searcherOptions, List evalOptions) throws Exception {
		logger.trace("Created wrapper for AS: {}/{}", asSearcherName, asEvaluatorName);
		logger.trace("Options for AS: \n\t{}\n\t{}", searcherOptions, evalOptions);
		this.asSearcherName = asSearcherName;
		this.asEvaluatorName = asEvaluatorName;
		this.searcherOptions = Options.splitStringIntoArr(searcherOptions);
		this.evalOptions =     Options.splitStringIntoArr(evalOptions);
		construct();
	}



	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void set_options_dict(Map options) throws Exception {
		if(options.containsKey("searcher") && asSearcherName == null) {
			Map searcherOptions = (Map) options.get("searcher");
			asSearcherName = (String) searcherOptions.get("type");
			this.searcherOptions = Options.flattenMapToArr((Map) searcherOptions.get("params"), true);
		}
		if(options.containsKey("evaluator") && asEvaluatorName == null) {
			Map evalOptions = (Map) options.get("evaluator");
			asEvaluatorName = (String) evalOptions.get("type");
			this.evalOptions = Options.flattenMapToArr((Map) evalOptions.get("params"), true);
		}
		construct();
	}

	private void construct() throws Exception {
		if(asEvaluatorName == null || asSearcherName == null) {
			return;
		}
		if(searcherOptions == null) {
			searcherOptions = new String[0];
		}
		if(evalOptions == null) {
			evalOptions = new String[0];
		}
		attributeSelection = new AttributeSelection();
		ASSearch searcher = (ASSearch) ConstructorUtils.invokeConstructor(Class.forName(asSearcherName));
		if(searcher instanceof OptionHandler) {
			((OptionHandler) searcher).setOptions(searcherOptions);
		}
		ASEvaluation eval = (ASEvaluation) ConstructorUtils.invokeConstructor(Class.forName(asEvaluatorName));
		if(eval instanceof OptionHandler) {
			((OptionHandler) eval).setOptions(evalOptions);
		}
		attributeSelection.setSearch(searcher);
		attributeSelection.setEvaluator(eval);
	}

	public void train(Instances instances) throws Exception {
		if(attributeSelection == null) {
			throw new IllegalStateException("Evaluator and Searcher were not set.");
		}
		attributeSelection.SelectAttributes(instances);
	}

	public Instances preprocess(Instances instances) throws Exception {
		if(attributeSelection == null) {
			throw new IllegalStateException("Evaluator and Searcher were not set.");
		}
		return attributeSelection.reduceDimensionality(instances);
	}
}
