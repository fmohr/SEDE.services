package de.upb.sede.services.mls;

import de.upb.sede.services.mls.util.MLDataSets;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * Make sure weather.arff is available.
 */
public class DataSetServiceTest {

	static final String testDataSetPath = "weather.arff";

	static Instances testDataSet = null;

	@BeforeClass
	public static void loadWeather() {
		testDataSet = Objects.requireNonNull(MLDataSets.getDataSet(testDataSetPath));
	}

	@After
	public void resetCache() {
		if(DataSetCache.contains(testDataSetPath)){
			DataSetCache.cache.remove(testDataSetPath);
		}
	}

	@Test
	public void createUnique() throws IOException {
		DataSetService dataSetService = DataSetService.createUnique(testDataSet);
		Instances loadedInstances = new DataSetService(dataSetService.getDataSetPath()).all();
		assertEquals(testDataSet, loadedInstances);
	}

	@Test
	public void createNamed() throws IOException {
		assertFalse(DataSetCache.contains("cached/weather2.arff"));
		DataSetService dataSetService = DataSetService.createNamed(testDataSet, "cached/weather2.arff");
		assertTrue(DataSetCache.contains("cached/weather2.arff"));
		Instances loadedInstances = new DataSetService(dataSetService.getDataSetPath()).all();
		assertEquals(testDataSet, loadedInstances);
	}

	@Test
	public void all() {
		assertFalse(DataSetCache.contains(testDataSetPath));
		DataSetService dataSetService = new DataSetService(testDataSetPath);
		assertFalse(DataSetCache.contains(testDataSetPath));
		Instances instances = dataSetService.all();
		assertTrue(DataSetCache.contains(testDataSetPath));
		assertEquals(instances.toString().trim(), testDataSet.toString().trim());
	}

	@Test
	public void allLabeled() {
		assertFalse(DataSetCache.contains(testDataSetPath));
		DataSetService dataSetService = new DataSetService(testDataSetPath);
		assertFalse(DataSetCache.contains(testDataSetPath));
		Instances instances = dataSetService.allLabeled(-1);
		assertTrue(DataSetCache.contains(testDataSetPath));
		assertEquals(instances.toString().trim(), testDataSet.toString().trim());
		assertEquals(instances.numAttributes() -1, instances.classIndex());
	}

	@Test
	public void fromIndices() {
		List indices = Arrays.asList(1,0,4,3);
		DataSetService dataSetService = new DataSetService(testDataSetPath);
		Instances instances = dataSetService.fromIndices(indices);
		/*
			Values manually read from weather.arff
		 */
		assertEquals(80., instances.get(0).value(1), 0.);
		assertEquals(85., instances.get(1).value(1), 0.);
		assertEquals(68., instances.get(2).value(1), 0.);
		assertEquals(70., instances.get(3).value(1), 0.);

		instances.remove(instances.firstInstance());
	}

	@Test
	public void fromIndicesLabeled() {
		List indices = Arrays.asList(1,0,4,3);
		DataSetService dataSetService = new DataSetService(testDataSetPath);
		Instances instances = dataSetService.fromIndicesLabeled(indices, -1);
		/*
			Values manually read from weather.arff
		 */
		assertEquals(80., instances.get(0).value(1), 0.);
		assertEquals(85., instances.get(1).value(1), 0.);
		assertEquals(68., instances.get(2).value(1), 0.);
		assertEquals(70., instances.get(3).value(1), 0.);
		// check class values:
		assertEquals(1., instances.get(0).classValue(), 0.);
		assertEquals(1., instances.get(1).classValue(), 0.);
		assertEquals(0., instances.get(2).classValue(), 0.);
		assertEquals(0., instances.get(3).classValue(), 0.);
	}

	@Test
	public void integrationTest() throws IOException {

		Instances newDataSet = new Instances(Objects.requireNonNull(MLDataSets.getDataSet(testDataSetPath)), 1);
		int numAttributes = newDataSet.numAttributes();
		Instance additionalInstnace = new DenseInstance(numAttributes);
		additionalInstnace.setValue(0, 2.);
		additionalInstnace.setValue(1, 2.);
		additionalInstnace.setValue(2, 1.);
		additionalInstnace.setValue(3, 1.);
		additionalInstnace.setValue(4, 1.);
		additionalInstnace.setDataset(newDataSet);
		newDataSet.add(additionalInstnace);
		DataSetService s1 = DataSetService.createNamed(newDataSet, "cached/newweather.arff");
		assertTrue(DataSetCache.contains("cached/newweather.arff"));
		assertSame(newDataSet, s1.all());
		DataSetCache.cache.remove("cached/newweather.arff");
		assertNotSame(newDataSet, s1.all());
		assertEquals(newDataSet.toString().trim(), s1.all().toString().trim());
	}
}