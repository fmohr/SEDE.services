package de.upb.sede.services.mls;

import de.upb.sede.services.mls.util.MLDataSets;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;

public class Labeler {

	/**
	 * Labels the given dataset by replacing the original class attribute and setting the class value of each feature.
	 * @param dataset (Unlabeled) dataset that will be labeled in-place.
	 * @param labels labels to be used on the features.
	 */
	public static void labelDataset(Instances dataset, List<String> labels) {
		if(dataset.size() != labels.size()) {
			throw new RuntimeException("Size mismatch, labels: " + labels.size() + ", dataset: " + dataset.size());
		}
		int classIndex = dataset.classIndex();
		if(classIndex<0) {
			throw new RuntimeException("Dataset has no class attribute defined.");
		}
		List<String> categories = categorize(labels);

		Attribute attribute = new Attribute("class", categories);
		dataset.replaceAttributeAt(attribute, classIndex);

		for (int i = 0; i < dataset.size(); i++) {
			dataset.get(i).setClassValue(labels.get(i));
		}
	}

	/**
	 * Sets the dataset class attribute index. -1 maps to the last attribute.
	 * @param dataset dataset that will get a class attribute index
	 * @param index index of the class attribute
	 */
	public static void setClassIndex(Instances dataset, int index) {
		MLDataSets.setModuloClassIndex(dataset, index);
	}

	/**
	 * Maps the classindices back to their class label which is implicitly assumed to be strings.
	 * @param categories categories of labels
	 * @param classIndices list of number representing indices.
	 */
	public static List<String>  classIndicesToNames(List<String> categories, List<Number> classIndices) {
		/*
		 * convert indices back to labels:
		 */
		return classIndices.stream().map(index -> categories.get(index.intValue())).collect(Collectors.toList());
	}

	/**
	 * Prettifies the given list of numbers by translating each number i to "Group i".
	 * @param classIndices indices of classes in number
	 * @return list of labels
	 */
	public static List<String> classIndicesPrettify(List<Number> classIndices) {
		return classIndices.stream().map(index -> String.format("Group %d", index.intValue()))
				.collect(Collectors.toList());
	}

	/**
	 * Categorizes the given list of labels by eliminating duplicates and sorting them alphabetically.
	 */
	public static List<String> categorize(List<String> labels) {
		/*
		 * remove duplicate values and order them alphabetically.
		 */
		Set<String> set =  set = new TreeSet<String>(labels);
		/*
		 * Copy them into a list so one can access them by index.
		 */
		List<String> categories = new ArrayList<>(set);
		return categories;
	}

}
