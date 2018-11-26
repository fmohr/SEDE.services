package de.upb.crc901.automl.pipeline.service;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.classifiers.Classifier;
import weka.core.OptionHandler;

import java.io.Serializable;
import java.util.*;

/**
 *
 * A class that helps 'plan' a ML-pipeline.
 *
 * Currently a ML-pipeline only exists of a series of AttributeSelection's (0 or more) followed by a
 * single classifier.
 *
 * @author aminfaez
 *
 */
@SuppressWarnings("serial")
public class MLPipelinePlan implements Serializable {

	// list of preprocessors
	private List<MLPipe> atrPipes = new LinkedList<>();

	// Points to the end of the pipeline.
	private MLPipe cPipe;

	/*
	 * Do nothing. Keeping this function to remain compatible.
	 */
	public MLPipelinePlan onHost(final String hostname) {
		return this;
	}

	/*
	 * Do nothing. Keeping this function to remain compatible.
	 */
	public MLPipelinePlan onHost(final String host, final int port) {
		return this;
	}

	public MLPipe addAttributeSelection(final String classname) {
		MLPipe asPipe = new MLPipe(Objects.requireNonNull(classname));
		this.atrPipes.add(asPipe); // add to pipe list before returning.
		return asPipe;
	}

	public void addOptions(final MLPipe pipe, final String option, final Object value) {
		pipe.addOptions(option + " " + value.toString());
	}

	public WekaAttributeSelectionPipe addWekaAttributeSelection(final ASSearch searcher, final ASEvaluation eval) {
		Objects.requireNonNull(searcher);
		Objects.requireNonNull(eval);
		WekaAttributeSelectionPipe asPipe = new WekaAttributeSelectionPipe();
		asPipe.withSearcher(searcher.getClass().getName());
		if (searcher instanceof OptionHandler) {
			String searchOptions[] = ((OptionHandler) searcher).getOptions();
			asPipe.addSearchOptions(searchOptions);
		}
		asPipe.withEval(eval.getClass().getName());
		if (eval instanceof OptionHandler) {
			String evalOptions[] = ((OptionHandler) eval).getOptions();
			asPipe.addEvalOptions(evalOptions);
		}
		this.atrPipes.add(asPipe);
		return asPipe;
	}

	public MLPipe setClassifier(final String classifierName) {
		this.cPipe = new MLPipe(classifierName); // set cPipe field.
		return this.cPipe;
	}

	public MLPipe setClassifier(final Classifier wekaClassifier) {
		Objects.requireNonNull(wekaClassifier);
		String classname = wekaClassifier.getClass().getName();
		this.cPipe = new MLPipe(classname);
		if (wekaClassifier instanceof OptionHandler) {
			String[] options = ((OptionHandler) wekaClassifier).getOptions();
			this.cPipe.addOptions(options);
		}
		return this.cPipe;
	}

	/**
	 * Returns True if the plan is 'valid' in the sense that a classifier was set.
	 */
	public boolean isValid() {
		if (this.cPipe == null) { // if classifier is null return false immediately
			return false;
		}
		for (MLPipe pipe : this.atrPipes) {
			if (!pipe.isValid()) {
				return false;
			}
		}
		return true;
	}

	public List<MLPipe> getAttrSelections() {
		return this.atrPipes;
	}

	public MLPipe getClassifierPipe() {
		return this.cPipe;
	}

	/**
	 * Creates the composition of this plan which includes the creation training and prediction.
	 * Only use this for logging and debugging.
	 * @return composition of this plan
	 */
	public static String debugCompositionString(MLPipelinePlan plan) {
		// TODO
		throw new RuntimeException("");
	}

	// CLASSES for pipe creation.
	public static abstract class AbstractPipe implements Serializable {

		protected boolean isValid() {
			return true;
		}
	}

	public static class MLPipe extends AbstractPipe {
		private final String classifierName;
		private final List<String> classifierOptions = new ArrayList<>();
		private final List<Object> constructorArgs = new ArrayList<>();

		protected MLPipe(final String classifierName) {
			this.classifierName = Objects.requireNonNull(classifierName);
		}

		public MLPipe addOptions(final String... additionalOptions) {
			Objects.requireNonNull(additionalOptions);
			for (String newOption : additionalOptions) {
				this.classifierOptions.add(newOption);
			}
			return this;
		}

		public MLPipe addConstructorArgs(final Object... args) {
			Objects.requireNonNull(args);
			for (Object newArg : args) {
				this.constructorArgs.add(newArg);
			}
			return this;
		}

		public String getName() {
			return this.classifierName;
		}

		public String getQualifiedName() {
			return this.classifierName;
		}

		public ArrayList<String> /* ArrayList was explicitly used */ getOptions() {
			ArrayList<String> options = new ArrayList<>();
			options.addAll(this.classifierOptions);
			return options;
		}

		public Object[] getArguments() {
			return this.constructorArgs.toArray();
		}

		public String getStringEncoding() {
			return this.getQualifiedName() + this.getOptions().toString();
		}
	}

	public static class WekaAttributeSelectionPipe extends MLPipe {
		private String searcherName, evalName;
		public static final String classname = "weka.attributeSelection.AttributeSelection";

		protected WekaAttributeSelectionPipe() {
			super(classname);
		}

		private List<String> searcherOptions = new ArrayList<>(), evalOptions = new ArrayList<>();

		public WekaAttributeSelectionPipe withSearcher(final String searcherName) {
			this.searcherName = Objects.requireNonNull(searcherName);
			return this;
		}

		public WekaAttributeSelectionPipe withEval(final String evaluator) {
			this.evalName = Objects.requireNonNull(evaluator);
			return this;
		}

		public WekaAttributeSelectionPipe addSearchOptions(final String... additionalOptions) {
			this.addToOptionList(this.searcherOptions, additionalOptions);
			return this;
		}

		public WekaAttributeSelectionPipe addEvalOptions(final String... additionalOptions) {
			this.addToOptionList(this.evalOptions, additionalOptions);
			return this;
		}

		private void addToOptionList(final List<String> optionList, final String[] additionalOptions) {
			Objects.requireNonNull(additionalOptions);
			for (String newOption : additionalOptions) {
				optionList.add(newOption);
			}
		}

		public String getSearcher() {
			return this.searcherName;
		}

		public String getEval() {
			return this.evalName;
		}

		@Override
		public String getQualifiedName() {
			return this.searcherName + "/" + this.evalName;
		}

		public ArrayList<String> getSearcherOptions() {
			ArrayList<String> options = new ArrayList<>();
			options.addAll(this.searcherOptions);
			return options;
		}

		public ArrayList<String> getEvalOptions() {
			ArrayList<String> options = new ArrayList<>();
			options.addAll(this.evalOptions);
			return options;
		}

		@Override
		protected boolean isValid() {
			if (this.isWekaAS() && (this.searcherName == null || this.evalName == null)) {
				return false;
			}
			return true;
		}

		public boolean isWekaAS() {
			return "weka.attributeSelection.AttributeSelection".equals(this.getName());
		}

		@Override
		public String getStringEncoding() {
			return this.searcherName + this.searcherOptions.toString() + "/" + this.evalName + this.evalOptions.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (MLPipe pipe : this.atrPipes) {
			if (sb.length() > 0) {
				sb.append(";");
			}
			if (pipe instanceof WekaAttributeSelectionPipe) {
				WekaAttributeSelectionPipe cPipe = (WekaAttributeSelectionPipe) pipe;
				sb.append(cPipe.getSearcher() + cPipe.getSearcherOptions().toString() + "/" + cPipe.getEval() + cPipe.getEvalOptions());
			} else {
				sb.append(pipe.getName() + pipe.getOptions().toString());
			}
		}
		if (sb.length() > 0) {
			sb.append(" -> ");
		}
		if (this.cPipe != null) {
			sb.append(this.cPipe.getName() + this.cPipe.getOptions().toString());
		} else {
			sb.append("NULL");
		}
		return sb.toString();
	}
}
