package de.upb.sede.services.mls;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.sede.services.mls.util.MLDataSets;
import weka.core.Instances;

public class DataSetService implements Serializable {

	private final static Logger logger = LoggerFactory.getLogger("MLS");

	private final String dataSetPath;

	public DataSetService(String filePath) {
		this.dataSetPath = filePath;
	}

	/**
	 * Return a new DataSet Service instance from the given dataset.
	 * This method generates a random dataSet name and delegates to the createNamed method.
	 * @param dataSet Dataset to be wrapped into a service.
	 * @return a unique DataSetService instance.
	 */
	public static DataSetService createUnique(Instances dataSet) throws IOException {
		String dataSetName = "cached/" + UUID.randomUUID().toString().substring(0, 5) + ".arff";
		return createNamed(dataSet, dataSetName);
	}

	/**
	 * Return a new named DataSet Service instance from the given dataset.
	 * The dataset will be cached and written back onto the disk.
	 * One can access the same dataset using the default constructor with the same name.
	 *
	 * If the dataset name already existed prior to the invocation the old dataset
	 * will be deleted and replaced by this one. This also affects old data set service instances.
	 * To avoid this use a unique name or call createUnique which chooses a name by random.
	 *
	 * @param dataSet Dataset to be wrapped into a service.
	 * @param dataSetName Dataset name
	 * @return
	 */
	public synchronized static DataSetService createNamed(Instances dataSet, String dataSetName) throws IOException {
//		dataSetName = "cached/" + dataSetName;
		if(DataSetCache.cache.containsKey(dataSetName)){
			logger.warn("New data set replaces old one: {}", dataSetName);
		} else{
			logger.info("Creating a new data set: {}", dataSetName);
			/*
				Write dataset into storage:
			 */
			MLDataSets.storeDataSet(dataSet, dataSetName);
		}
		DataSetCache.cache.put(dataSetName, dataSet);
		return new DataSetService(dataSetName);
	}

	/**
	 * Retreives the data set from cache and returns it as an Instances object.
	 * If the dataset isn't cached its loaded from dataSetpath.
	 *
	 * @return Instances data set
	 */
	public Instances all() {
		/**
		 * Make sure the data is cached:
		 */
		if(! DataSetCache.contains(dataSetPath)){
			DataSetCache.loadDataSet(dataSetPath);
		}
		return Objects.requireNonNull(DataSetCache.cache.get(this.dataSetPath));
	}

	/**
	 * Retreives the data set from cache and returns it as an Instances object.
	 * The returned instances object has the given class index. Use -1 for last attribute.
	 * If the dataset isn't cached its loaded from dataSetpath.
	 *
	 * @param classIndex
	 *
	 * @return Instances data set
	 */
	public Instances allLabeled(int classIndex) {
		Instances dataSet = all();
		MLDataSets.setModuloClassIndex(dataSet, classIndex);
		return dataSet;
	}

	/**
	 *
	 * @param indices
	 * @return
	 */
	public Instances fromIndices(List<Number> indices) {
		/*
			Parse to ints:
		 */
		List<Integer> intIndices = new ArrayList<>();
		indices.forEach(n -> intIndices.add(n.intValue()));
		Instances originalDataSet = all();
		IndexedInstances ii = new IndexedInstances(originalDataSet, dataSetPath, intIndices);
		return ii;
	}

	public Instances fromIndicesLabeled(final List indices, final int classIndex) {
		Instances dataSet = fromIndices(indices);
		MLDataSets.setModuloClassIndex(dataSet, classIndex);
		return dataSet;
	}

	public String getDataSetPath(){
		return dataSetPath;
	}

}
/*
	Keep the cache in a separate class to prevent it from being serialized when the DataSetService instances are.
	Contains a singletion cache
 */
class DataSetCache {
	transient final static Map<String, Instances> cache;

	static {
		/*
			ConcurrentHashMap to synchronize access.
			However the reference to this map isn't exposed by the datasetService.
			So if the access onto the map is synchronized by this class, a normal HashMap can be used instead.
		 */
		cache = new ConcurrentHashMap<>();
	}

	synchronized static void loadDataSet(String dataSetPath) {
		if(!contains(dataSetPath)){
			/*
				Load the data set from storage:
			 */
			Instances instances = MLDataSets.getDataSet(dataSetPath);
			DataSetCache.cache.put(dataSetPath, Objects.requireNonNull(instances));
		}
	}

	 static boolean contains(String dataSetPath) {
		return cache.containsKey(dataSetPath);
	}

}