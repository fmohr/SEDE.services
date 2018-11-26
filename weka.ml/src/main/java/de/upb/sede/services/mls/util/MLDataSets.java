package de.upb.sede.services.mls.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ml.data.LabeledInstancesCaster;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

public class MLDataSets {

	private static final Logger logger = LoggerFactory.getLogger("MLS");

	private static final String DATASET_PATH;

	static {
		/*
		 * Read the path of data set folder from environment variable: 'DATASET_PATH'
		 */
		if(System.getenv().containsKey("DATASET_PATH") && System.getenv().get("DATASET_PATH")!=null) {
			String environmentDatasetPath = System.getenv().get("DATASET_PATH");
			if(!environmentDatasetPath.endsWith("/")) {
				environmentDatasetPath += "/";
			}
			DATASET_PATH = environmentDatasetPath;
		} else {
			DATASET_PATH = "../datasets/";
			logger.info("Environment variable 'DATASET_PATH' isn't defined.");
			logger.info("Using {} as default path to data sets. Change default in: MLDataSets.java",
					DATASET_PATH);
		}
		File datasetDirectory = new File(DATASET_PATH);
		if(!datasetDirectory.exists() || !datasetDirectory.isDirectory()){
			logger.error("Dataset folder {} doesn't exist.", datasetDirectory.getAbsolutePath());
		}
	}

	/**
	 * Reads all the instances from the file (ARFF, CSV, XRFF, ...) on the given filepath.
	 *
	 * @param relativeFilePath dataset file path relative to 'DATASET_PATH'.
	 * @return loaded dataset
	 */
	public static Instances getDataSet(String relativeFilePath) {
		String dataSetPath = getAbsolutePathFromRelative(relativeFilePath);
		try {
			ConverterUtils.DataSource source = new ConverterUtils.DataSource(dataSetPath);
			Instances instances = source.getDataSet();
			return instances;
		} catch (Exception e) {
			logger.error("Exception during loading dataset from folder " + dataSetPath, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads all the instances from the file (ARFF, CSV, XRFF, ...) on the given filepath.
	 * Marks the classIndex'th attribute as the class attribute of the dataset.
	 *
	 * @param relativeFilePath dataset file path relative to 'DATASET_PATH'.
	 * @param classIndex class index of the dataset. Negative values start from end. e.g.: -1 is last index.
	 * @return loaded dataset
	 */
	public static Instances getClassifiedDataSet(String relativeFilePath, int classIndex) {
		Instances dataset = getDataSet(relativeFilePath);
		setModuloClassIndex(dataset, classIndex);
		return dataset;
	}

	/**
	 * Reads all the instances from the file (ARFF, CSV, XRFF, ...) on the given filepath.
	 * Marks the last attribute as the class attribute of the dataset.
	 *
	 * @param relativeFilePath dataset file path relative to 'DATASET_PATH'.
	 * @return loaded dataset
	 */
	public static Instances getDataSetWithLastIndexClass(String relativeFilePath) {
		return getClassifiedDataSet(relativeFilePath, -1);
	}

	/**
	 * @param instances
	 * @param relativeFilePath
	 */
	public synchronized static void storeDataSet(Instances instances, String relativeFilePath) throws IOException {
		String dataSetPath = getAbsolutePathFromRelative(relativeFilePath);
		File dataSetFile = new File(dataSetPath);
		/*
			Make sure the parent directory exists:
		 */
		File parentDirectory = dataSetFile.getParentFile();
		if(!parentDirectory.exists()) {
			if(!parentDirectory.mkdirs()) {
				throw new RuntimeException("Cannot create the necessary directory to store the instances: "
						+ dataSetFile.getAbsolutePath());
			}
		} else if(!parentDirectory.isDirectory()) {
			throw new RuntimeException("Illegal path:" + dataSetFile.getAbsolutePath());
		}
		try(OutputStream out = new FileOutputStream(dataSetFile)) {
			new LabeledInstancesCaster().cts_LabeledInstances(out, instances);
		}
	}

	public static String getAbsolutePathFromRelative(String relativeFilePath) {
		return new File(DATASET_PATH + relativeFilePath).getAbsolutePath();
	}

	/*
	 * same as setClassIndex but e.g. -1 would be resolved to the last attribute.
	 */
	public static void setModuloClassIndex(Instances dataset, int classIndex) {
		int dataAttrCount = dataset.numAttributes();
		/*
			modulo enforces classIndex to be in bound.
			also converts negative indices to the corresponding offset index from the end:
			e.g.: -1 % 5 > 4
		 */
		while(classIndex<0 || classIndex>= dataAttrCount) {
			classIndex = classIndex % dataAttrCount;
			classIndex = dataAttrCount + classIndex;
		}
		dataset.setClassIndex(classIndex);
	}

	public static List<Integer> intRange(int beginInclusive, int endExclusive) {
		List<Integer> list = new ArrayList<>();
		for (int i = beginInclusive; i < endExclusive; i++) {
			list.add(i);
		}
		return list;
	}
	public static <T>List<List<T>> split(List<T> baseLists, double firstPercentage, double... restPercentages){
		List<Double> percentages = new ArrayList<>();
		percentages.add(firstPercentage);
		double currentPercent = firstPercentage;
		for (int i = 0; i < restPercentages.length; i++) {
			currentPercent += restPercentages[i];
			percentages.add(currentPercent);
		}

		double size = baseLists.size();
		Supplier<Integer> currentBound = () -> {
			if(percentages.isEmpty()){
				return (int)size;
			} else {
				return (int) (percentages.get(0) * size);
			}
		};

		List<List<T>> lists = new ArrayList<>();
		lists.add(new ArrayList<>());
		for (int i = 0; i < size; i++) {
			while(i > currentBound.get()) {
				lists.add(new ArrayList<>());
				percentages.remove(0);
			}
			lists.get(lists.size()-1).add(baseLists.get(i));
		}
		return lists;
	}



	public static List<Integer> isInRange(List<Integer> list, int beginInclusive, int endExclusive) {
		for (Number item : list) {
			int currentItem = item.intValue();
			if (currentItem < beginInclusive || currentItem >= endExclusive) {
				throw new IndexOutOfBoundsException
						(currentItem + " not in: [" + beginInclusive + ", " + (endExclusive - 1) + "]");
			}
		}
		return list;
	}

	public static int isInRange(int item, int beginInclusive, int endExclusive) {
		if(item < beginInclusive || item >= endExclusive) {
			throw new IndexOutOfBoundsException
					(item + " not in: [" + beginInclusive + ", " + (endExclusive-1) + "]");
		}
		return item;
	}

	/**
	 * Given a list of predicted classes this method returns a list that contains the indices of the classified labels.
	 * The given list can either already contain class indices which are objects of type java.lang.Number. In this case the list is just copied into a new list.
	 * Or it may also contain Strings which indicate categorical labels of the class feature. The returned list is will contain the index of each predicted category.
	 */
	public static List<Integer> toClassIndexPredictions(List servicePredictions, Attribute classAttribute) {
		Objects.requireNonNull(servicePredictions);
		List<Integer> indices = new ArrayList<>();
		if(servicePredictions.isEmpty()) {
			return  indices;
		}
		else {
			for(Object prediction : servicePredictions) {
				if(prediction instanceof  Number) {
					indices.add(((Number) prediction).intValue());
				} else if(prediction instanceof String) {
					int classIndex = classAttribute.indexOfValue((String) prediction);
					indices.add(classIndex);
				} else {
					throw new RuntimeException("Cannot handle predictions of type: " + prediction.getClass().getName());
				}
			}
			return indices;
		}
	}

	public static int countMatchPredictions(List servicePredicitions, Instances testset) {
		Objects.requireNonNull(servicePredicitions);
		Objects.requireNonNull(testset);
		if(testset.size() != servicePredicitions.size()){
			throw new RuntimeException("Dimension mismatch: " + servicePredicitions.size() + " predictions, " + testset.size() + " testset size.");
		}
		List<Integer> indices = toClassIndexPredictions(servicePredicitions, testset.classAttribute());
		int correctPredictions = 0;
		for (int i = 0; i < testset.size(); i++) {
			Instance testInstance = testset.get(i);
			int actualIndex = (int)testInstance.classValue();
			if(actualIndex == indices.get(i)) {
				correctPredictions++;
			}
		}
		return correctPredictions;
	}
}
