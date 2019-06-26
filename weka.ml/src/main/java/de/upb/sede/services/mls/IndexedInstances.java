package de.upb.sede.services.mls;

import de.upb.sede.services.mls.DataSetService;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static de.upb.sede.services.mls.util.MLDataSets.*;

/**
 *
 * Wraps an instances object and offers an indexed view onto it.<br>
 *     It is especially useful in the context of auto machine learning where usually there is a specific dataset which is split and shuffled multiple times.
 *     This class enables one to load the data to memory only once but have multiple (optionally shuffled) subsets with far less memory impact. <br>
 *
 * Use-case: Instead of splitting a huge data set into fractions which would copy the data and double the amount of memory used,
 * one can wrap the data set into an instance of this class and specify a subset by giving a list of indices. <br>
 * For example: (cifar.arff, [0,2,4,6,8,..]) would reference the cifar dataset, but when asked for the 3rd instance,
 * the 4th instance of the original data set is returned.<br><br>
 * This class guarantees that the wrapped instances object remains untouched. This means that every method invocation which
 * would change attributes or instances (adding, renaming, ect.), will throw an exception. <br>
 * Any other query over attributes or data is allowed. From outside the instances of this class behave
 * as if the actual dataset was copied and then filtered by the given indices.<br><br>
 *
 * The datasetReference field can be used to denote which original data set is used. <br>
 *     For example all objects of type IndexedInstances whose datasetReference is set to "cifar.arff",
 *     (should) have an identical innerData field which is the cifar data set. <br><br>
 *
 * IndexedInstances Objects never wrap other IndexedInstances objects. (An exception is raised in case one tries to.)
 * While stacking layers of indexed instances could lead to interesting features like 'live immutable views',
 * due to the possibility of long access time, invisible side-effects and the headache of complex data structures,
 * it was decided to not go that route. <br>
 * Note: one can still instantiate objects of this class using another IndexedInstances object as the 'wrapped' data set,
 * by using the constructors which accepts IndexedInstances or by using the flattenLayer method on the IndexedInstances object.
 * These methods apply the given index list onto the given indexedinstances' index list to flatten the view on the original data set.
 *
 * @author Amin F
 */
public class IndexedInstances extends Instances {

	/**
	 * Note that the innerData field should never be another indexed instances,
	 * or else access time drops for each layer of indexed instances.
	 * There are constructors dedicated for accepting IndexesInstances.
	 */
	private final Instances innerData;
	private final String datasetReference;
	private final List<Integer> indices;

	public IndexedInstances(Instances dataset, String datasetReference, List<Integer> indices, Optional<Integer> classIndex) {
		super(dataset, 0);
		if(dataset instanceof IndexedInstances) {
			throw new RuntimeException("Indexed instances should be flattened out not wrapped inside of another indexed instances. " +
					"Use constructors accepting IndexedInstances or call flattenLayer on the given dataset instead. data set reference: " + datasetReference);
		}
		this.innerData = dataset;
		this.datasetReference = datasetReference;
		this.indices = Objects.requireNonNull(indices);
		if(innerData.classIndex() >= 0) {
			setModuloClassIndex(this, innerData.classIndex());
		}
		if(classIndex.isPresent()) {
			setModuloClassIndex(this, classIndex.get());
		}
	}

	/**
	 * Wraps the entire given data set.
	 * From outside it behaves an immutable copy of the given Instances object.
	 */
	public IndexedInstances(Instances dataset) {
		this(dataset, dataset.relationName(), intRange(0, dataset.size()), Optional.empty());
	}

	/**
	 * Wraps the entire given data set.
	 * From outside it behaves an immutable copy of the given Instances object. <br>
	 * Additionally it sets a dataset reference.
	 */
	public IndexedInstances(Instances dataset, String datasetReference) {
		this(dataset, datasetReference, intRange(0, dataset.size()), Optional.empty());
	}

	public IndexedInstances(Instances dataset, String datasetReference, List<Integer> subSetIndexList) {
		this(dataset, datasetReference, isInRange(subSetIndexList,0 , dataset.size()), Optional.empty());
	}

	public IndexedInstances(Instances dataset, String datasetReference, List<Integer> subSetIndexList,int classIndex) {
		this(dataset, datasetReference, isInRange(subSetIndexList,0 , dataset.size()), Optional.of(classIndex));
	}

	/**
	 * The resulting instance is a copy of the given one.
	 */
	public IndexedInstances(IndexedInstances innerLayer) {
		this(innerLayer.getInnerDataset(), innerLayer.getDatasetReference(), new ArrayList<>(innerLayer.indices));
	}

	/**
	 * The returned IndexedInstances will wrap the dataset which is also wrapped by the given innerLayer by
	 * flattening the given subsetIndexList.
	 */
	public IndexedInstances(IndexedInstances innerLayer, List<Integer> subSetIndexList) {
		this(innerLayer.getInnerDataset(), innerLayer.getDatasetReference(), innerLayer.flattenIndices(subSetIndexList));
	}

	/*
	 * Creates an instances object which is a copy of this.
	 * The returned instances won't be of type IndexedInstances, thus it will mutable.
	 */
	public Instances toMutableInstances() throws IOException {
		DataSetService dataSetService = DataSetService.createNamed(innerData, datasetReference);
		Instances clonedDataSet = new Instances(innerData, numAttributes());
		for(Instance instance : this){
			Instance instanceCopy = (Instance) instance.copy();
			instanceCopy.setDataset(clonedDataSet);
			clonedDataSet.add(instanceCopy);
		}
		if(this.classIndex()>=0){
			clonedDataSet.setClassIndex(this.classIndex());
		}
		return clonedDataSet;
	}

	public List<Integer> flattenIndices(List<Integer> outerLayer) {
		List<Integer> flatIndices = new ArrayList<>();
		for (Integer index : outerLayer) {
			flatIndices.add(indices.get(index));
		}
		return flatIndices;
	}

	public Instances flattenLayer(List<Integer> outerLayer) {
		return new IndexedInstances(innerData, datasetReference, flattenIndices(outerLayer));
	}

	public String getDatasetReference() {
		return datasetReference;
	}

	public void setIndices(List<Integer> indices) {
		this.indices.clear();
		indices.addAll(isInRange(indices, 0, this.numInstances()));
	}

	/**
	 * Use this method cautiously as it exposes the inner dataset.
	 * Changing it might break this indexed instances object.
	 */
	public Instances getInnerDataset() {
		return innerData;
	}

	public void addIndex(int index){
		indices.add(isInRange(index, 0, this.numInstances()));

	}

	public List<Integer> getIndices() {
		return indices;
	}

	public String toReferenceString() {
		return datasetReference + ":" + indices.size();
	}

	public String toString() {
		return super.toString();
	}

	private void mutableOperationInvoced() {
		throw new RuntimeException("Cannot mutate instances of type: IndexedInstances " + toReferenceString()
				+ ". If it's absolutely necessary, use toMutableInstances first.");
	}


	/*
		Overwrite every method from the Instances class and delegate some of them to the inner dataset.
	 */


	@Override
	public void setClassIndex(int index){
		super.setClassIndex(index);
		innerData.setClassIndex(index);
	}

	@Override
	public void sort(int attIndex) {
		mutableOperationInvoced();
	}

	@Override
	public void sort(Attribute att) {
		mutableOperationInvoced();
	}

	@Override
	public void stableSort(int attIndex) {
		mutableOperationInvoced();
	}

	@Override
	public void stableSort(Attribute att) {
		mutableOperationInvoced();
	}

	@Override
	public void stratify(int numFolds) {
		mutableOperationInvoced();
	}

	@Override
	public double sumOfWeights() {
		return super.sumOfWeights();
	}

	@Override
	public Instances testCV(int numFolds, int numFold) {
		return super.testCV(numFolds, numFold);
	}

	@Override
	public Instances trainCV(int numFolds, int numFold) {
		return super.trainCV(numFolds, numFold);
	}

	@Override
	public Instances trainCV(int numFolds, int numFold, Random random) {
		return super.trainCV(numFolds, numFold, random);
	}

	@Override
	public double[] variances() {
		return super.variances();
	}

	@Override
	public double variance(int attIndex) {
		return super.variance(attIndex);
	}

	@Override
	public double variance(Attribute att) {
		return super.variance(att);
	}

	@Override
	public AttributeStats attributeStats(int index) {
		return super.attributeStats(index);
	}

	@Override
	public double[] attributeToDoubleArray(int index) {
		return innerData.attributeToDoubleArray(indices.get(index));
	}

	@Override
	public String toSummaryString() {
		return super.toSummaryString();
	}

	@Override
	public void swap(int i, int j) {
		int index = indices.get(i);
		indices.set(i, indices.get(j));
		indices.set(j, index);
	}

	@Override
	public String getRevision() {
		return innerData.getRevision();
	}


	@Override
	public Instances stringFreeStructure() {
		return super.stringFreeStructure();
	}

	@Override
	public boolean add(Instance instance) {
		mutableOperationInvoced();
		return false;
	}

	@Override
	public void add(int index, Instance instance) {
		mutableOperationInvoced();
	}

	@Override
	public Attribute attribute(int index) {
		return super.attribute(index);
	}

	@Override
	public Attribute attribute(String name) {
		return super.attribute(name);
	}

	@Override
	public boolean checkForAttributeType(int attType) {
		return super.checkForAttributeType(attType);
	}

	@Override
	public boolean checkForStringAttributes() {
		return super.checkForStringAttributes();
	}

	@Override
	public boolean checkInstance(Instance instance) {
		return super.checkInstance(instance);
	}

	@Override
	public Attribute classAttribute() {
		return super.classAttribute();
	}

	@Override
	public int classIndex() {
		return super.classIndex();
	}

	@Override
	public void compactify() {
		innerData.compactify();
	}

	@Override
	public void delete() {
		indices.clear();
	}

	@Override
	public void delete(int index) {
		indices.remove(index);
	}

	@Override
	public void deleteAttributeAt(int position) {
		mutableOperationInvoced();
	}

	@Override
	public void deleteAttributeType(int attType) {
		mutableOperationInvoced();
	}

	@Override
	public void deleteStringAttributes() {
		mutableOperationInvoced();
	}

	@Override
	public void deleteWithMissing(int attIndex)  {
		super.deleteWithMissing(attIndex);
	}

	@Override
	public void deleteWithMissing(Attribute att)  {
		super.deleteWithMissing(att);
	}

	@Override
	public void deleteWithMissingClass() {
		super.deleteWithMissingClass();
	}

	@Override
	public Enumeration<Attribute> enumerateAttributes() {
		return super.enumerateAttributes();
	}

	@Override
	public Enumeration<Instance> enumerateInstances() {
		return new Enumeration<Instance>() {
			Iterator<Integer> innerIt = indices.iterator();
			@Override
			public boolean hasMoreElements() {
				return innerIt.hasNext();
			}

			@Override
			public Instance nextElement() {
				return innerData.instance(innerIt.next());
			}
		};
	}

	@Override
	public String equalHeadersMsg(Instances dataset) {
		return super.equalHeadersMsg(dataset);
	}

	@Override
	public boolean equalHeaders(Instances dataset) {
		return super.equalHeaders(dataset);
	}

	@Override
	public Instance firstInstance() {
		return instance(0);
	}

	@Override
	public Random getRandomNumberGenerator(long seed) {
		return super.getRandomNumberGenerator(seed);
	}

	@Override
	public void insertAttributeAt(Attribute att, int position) {
		mutableOperationInvoced();
	}

	@Override
	public Instance instance(int index) {
		return innerData.get(indices.get(index));
	}

	@Override
	public Instance get(int index) {
		return innerData.get(indices.get(index));
	}

	@Override
	public double kthSmallestValue(Attribute att, int k) {
		return super.kthSmallestValue(att, k);
	}

	@Override
	public double kthSmallestValue(int attIndex, int k) {
		return super.kthSmallestValue(attIndex, k);
	}

	@Override
	public Instance lastInstance() {
		return instance(numInstances()-1);
	}

	@Override
	public double meanOrMode(int attIndex) {
		return super.meanOrMode(attIndex);
	}

	@Override
	public double meanOrMode(Attribute att) {
		return super.meanOrMode(att);
	}

	@Override
	public int numAttributes() {
		return super.numAttributes();
	}

	@Override
	public int numClasses() {
		return super.numClasses();
	}

	@Override
	public int numDistinctValues(int attIndex) {
		return super.numDistinctValues(attIndex);
	}

	@Override
	public int numDistinctValues(Attribute att) {
		return super.numDistinctValues(att);
	}

	@Override
	public int numInstances() {
		return indices.size();
	}

	@Override
	public int size() {
		return indices.size();
	}

	@Override
	public void randomize(Random random) {
		super.randomize(random);
	}

	@Override
	@Deprecated
	public boolean readInstance(Reader reader) throws IOException {
		mutableOperationInvoced();
		return false;
	}

	@Override
	public void replaceAttributeAt(Attribute att, int position) {
		mutableOperationInvoced();
	}

	@Override
	public String relationName() {
		return super.relationName();
	}

	@Override
	public Instance remove(int index) {
		Instance i = instance(index);
		indices.remove(index);
		return i;
	}

	@Override
	public void renameAttribute(int att, String name) {
		super.renameAttribute(att, name);
	}

	@Override
	public void renameAttribute(Attribute att, String name) {
		super.renameAttribute(att, name);
	}

	@Override
	public void renameAttributeValue(int att, int val, String name) {
		mutableOperationInvoced();
	}

	@Override
	public void renameAttributeValue(Attribute att, String val, String name) {
		mutableOperationInvoced();
	}

	@Override
	public Instances resample(Random random) {
		return super.resample(random);
	}

	@Override
	public Instances resampleWithWeights(Random random) {
		return super.resampleWithWeights(random);
	}

	@Override
	public Instances resampleWithWeights(Random random, boolean[] sampled) {
		return super.resampleWithWeights(random, sampled);
	}

	@Override
	public Instances resampleWithWeights(Random random, boolean representUsingWeights) {
		return super.resampleWithWeights(random, representUsingWeights);
	}

	@Override
	public Instances resampleWithWeights(Random random, boolean[] sampled, boolean representUsingWeights) {
		return super.resampleWithWeights(random, sampled, representUsingWeights);
	}

	@Override
	public Instances resampleWithWeights(Random random, double[] weights) {
		return super.resampleWithWeights(random, weights);
	}

	@Override
	public Instances resampleWithWeights(Random random, double[] weights, boolean[] sampled) {
		return super.resampleWithWeights(random, weights, sampled);
	}

	@Override
	public Instances resampleWithWeights(Random random, double[] weights, boolean[] sampled, boolean representUsingWeights) {
		return super.resampleWithWeights(random, weights, sampled, representUsingWeights);
	}

	@Override
	public Instance set(int index, Instance instance) {
		mutableOperationInvoced();
		return null;
	}

	@Override
	public void setClass(Attribute att) {
		super.setClass(att);
	}

}
