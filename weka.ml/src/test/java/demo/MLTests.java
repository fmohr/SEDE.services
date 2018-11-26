package demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.upb.sede.BuiltinCaster;
import de.upb.sede.client.CoreClient;
import de.upb.sede.client.HttpCoreClient;
import de.upb.sede.config.ClassesConfig;
import de.upb.sede.config.ExecutorConfiguration;
import de.upb.sede.config.OnthologicalTypeConfig;
import de.upb.sede.core.ObjectDataField;
import de.upb.sede.core.SEDEObject;
import de.upb.sede.core.ServiceInstanceField;
import de.upb.sede.core.ServiceInstanceHandle;
import de.upb.sede.exec.ExecutorHttpServer;
import de.upb.sede.gateway.ExecutorHandle;
import de.upb.sede.gateway.GatewayHttpServer;
import de.upb.sede.requests.ExecutorRegistration;
import de.upb.sede.requests.Result;
import de.upb.sede.requests.RunRequest;
import de.upb.sede.requests.resolve.ResolvePolicy;
import de.upb.sede.services.mls.DataSetService;
import de.upb.sede.services.mls.util.MLDataSets;
import de.upb.sede.util.ExecutorConfigurationCreator;
import de.upb.sede.util.FileUtil;
import ml.data.LabeledInstancesCaster;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.instance.RemovePercentage;

public class MLTests {
	private final static Logger logger = LoggerFactory.getLogger(MLTests.class);

	static CoreClient coreClient;

	static String clientAddress = "localhost";
	static int clientPort = 7000;

	static String gatewayAddress = "localhost";
	static int gatewayPort = 6000;

	static ExecutorHttpServer executor1;
	static ExecutorHttpServer executor2;
	static ExecutorHttpServer executor3;

	static GatewayHttpServer gateway;

	static Instances dataset;
	static Instances weatherTrainSet;
	static Instances weatherTestSet;

	private static final String datasetRef = "secom.arff";

	private static final String pyExecutorId = "PY-Scikit-Executor";

	@BeforeClass
	public static void startClient() {
		gateway = new GatewayHttpServer(gatewayPort, getTestClassConfig(), getTestTypeConfig());

		ExecutorConfigurationCreator creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("executor-weka-bayesnet");
		creator.withSupportedServices(DataSetService.class.getName(), "weka.classifiers.bayes.BayesNet",
				"weka.classifiers.trees.RandomForest");
		ExecutorConfiguration configuration = ExecutorConfiguration.parseJSON(creator.toString());
		executor1 = new ExecutorHttpServer(configuration, "localhost",  9000);
		gateway.getBasis().register(executor1.getBasisExecutor().registration());

		creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("executor-weka-naivebayes");
		creator.withSupportedServices(DataSetService.class.getName(), "weka.classifiers.bayes.NaiveBayes");
		configuration = ExecutorConfiguration.parseJSON(creator.toString());
		executor2 = new ExecutorHttpServer(configuration, "localhost",  9001);
		gateway.getBasis().register(executor2.getBasisExecutor().registration());


		creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("executor-weka-normalize");
		creator.withSupportedServices(DataSetService.class.getName(),
				"weka.filters.unsupervised.attribute.Normalize");
		configuration = ExecutorConfiguration.parseJSON(creator.toString());
		executor3 = new ExecutorHttpServer(configuration, "localhost",  9002);
		gateway.getBasis().register(executor3.getBasisExecutor().registration());

		creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("Client");
		configuration = ExecutorConfiguration.parseJSON(creator.toString());
		coreClient = HttpCoreClient.createNew(configuration, clientAddress, clientPort, gatewayAddress, gatewayPort);
		/*
			Disable if you dont have dot installed.
		 */
		coreClient.writeDotGraphToDir("testrsc/ml");


	}

	/*
	 * Comment the annotation out if you dont have a python executor running on 'localhost:5000'.
	 */
	@BeforeClass
	public static void registerScikitExecutor() {
		// register the python executor
		String pythonExecutorConfig = ExecutorConfigurationCreator.newConfigFile()
				.withExecutorId(pyExecutorId)
				.withCapabilities("python")
				.withSupportedServices("sklearn.ensemble.RandomForestClassifier",
						"sklearn.gaussian_process.GaussianProcessClassifier",
						"sklearn.naive_bayes.GaussianNB",
						"tflib.NeuralNet")
				.withThreadNumberId(1).toString();
		ExecutorConfiguration pythonExecConfig = ExecutorConfiguration.parseJSON(pythonExecutorConfig);
		Map<String, Object> pythonExecutorContactInfo = new HashMap<>();
		pythonExecutorContactInfo.put("id", pyExecutorId);
		pythonExecutorContactInfo.put("host-address", "192.168.0.103:5000");

		ExecutorRegistration pythonExecutorRegistration = new ExecutorRegistration(pythonExecutorContactInfo,
				pythonExecConfig.getExecutorCapabilities(),
				pythonExecConfig.getSupportedServices());

		gateway.getBasis().register(pythonExecutorRegistration);
	}

//	@BeforeClass
	public static void blockTest() throws Exception {
		int confirmation = 1;
		while(confirmation!= 0) {
			Object[] choices = {"Continue", "Refresh", "Cancel"};
			Object defaultChoice = choices[1];
			String registeredExecutors = "";
			for(ExecutorHandle executorHandle : gateway.getBasis().getExecutorCoord().getExecutors()) {
				registeredExecutors += "\n" + executorHandle.getContactInfo().toString();
			}
			confirmation = JOptionPane.showOptionDialog(null,
					"Registered Exeuctors:" + registeredExecutors,
					"Registered executors",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					choices,
					defaultChoice);
			if(confirmation == 2) {
				System.exit(1);
			}
		}
	}

	@BeforeClass
	public static void loadDataSet() throws Exception {
		List<Number> numberList = new ArrayList<>();
		short[] pixels = new short[numberList.size()];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = numberList.get(i).shortValue();
		}

		dataset = MLDataSets.getDataSetWithLastIndexClass(datasetRef);

		Filter imputer = new ReplaceMissingValues();
		imputer.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, imputer);

		RemovePercentage splitter = new RemovePercentage();

		splitter.setInputFormat(dataset);
		splitter.setPercentage(90);
		weatherTrainSet = Filter.useFilter(dataset, splitter);

		splitter.setInputFormat(dataset);
		splitter.setInvertSelection(true);
		weatherTestSet = Filter.useFilter(dataset, splitter);
	}
	@AfterClass
	public static  void shutdownClient() {
		executor1.shutdown();
		executor2.shutdown();
		executor3.shutdown();
		coreClient.getClientExecutor().shutdown();
		gateway.shutdown();
	}

	@Test
	public void testClassification1() {
		String composition =
				"pp = weka.filters.unsupervised.attribute.Normalize::__construct();\n" +
				"pp::train({trainset});\n" +
				"trainset1 = pp::preprocess({trainset});\n" +
				"s1 = weka.classifiers.trees.RandomForest::__construct();\n" +
				"s1::train({trainset1});\n" +
				"testset1 = pp::preprocess({testset});\n" +
				"predictions = s1::predict({testset1});\n";

		logger.info("Test classification with composition: \n" + composition);

		ResolvePolicy policy = new ResolvePolicy();
		policy.setPersistentServices(Arrays.asList("s1"));
		policy.setReturnFieldnames(Arrays.asList("predictions"));

		SEDEObject trainset = getDataField( weatherTrainSet);
		SEDEObject testset = getDataField( weatherTestSet);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("trainset", trainset);
		inputs.put("testset", testset);

		RunRequest runRequest = new RunRequest("classification1", composition, policy, inputs);

		Map<String, Result> resultMap = coreClient.blockingRun(runRequest);
		Result result = resultMap.get("predictions");
		if(result == null || result.hasFailed()) {
			Assert.fail("Result missing...");
		}
		/*
			Cast it to List:
		 */
		 List prediction = (List) result.castResultData(
				"builtin.List", BuiltinCaster.class).getDataField();
		 double correctPredictions = 0.;
		for (int i = 0; i < prediction.size(); i++) {
			Instance testInstance = weatherTestSet.get(i);
			if(testInstance.classValue() == ((Number)prediction.get(i)).doubleValue()) {
				correctPredictions++;
			}
		}
		logger.info("{}/{} correct predictions.", (int) correctPredictions, prediction.size());
	}


	@Test
	/**
	 * This test assumes that a python executor is running on 'localhost:5000'.
	 * Comment the annotation out if you cant satisfy that.
	 */
	public void testClassification2() {
		String composition =
				"dataset = de.upb.sede.services.mls.DataSetService::__construct({\"" +datasetRef + "\"});" +
				"trainset = dataset::fromIndicesLabeled({indices_train, -1});" +
				"testset  = dataset::fromIndicesLabeled({indices_test, -1});" +
				"s1 = weka.classifiers.bayes.BayesNet::__construct();" +
				"s1::train({trainset});" +
				"predictions = s1::predict({testset});";

		logger.info("Test classification with composition: \n" + composition);

		ResolvePolicy policy = new ResolvePolicy();
		policy.setPersistentServices(Arrays.asList("s1"));
		policy.setReturnPolicy(ResolvePolicy.all);

		List<List<Integer>> splits = MLDataSets.split(MLDataSets.intRange(0, dataset.size()),0.5);


		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("indices_train", new ObjectDataField("builtin.List", splits.get(0)));
		inputs.put("indices_test", new ObjectDataField("builtin.List", splits.get(1)));

		RunRequest runRequest = new RunRequest("classification2", composition, policy, inputs);

		Map<String, Result> resultMap = coreClient.blockingRun(runRequest);
		Result result = resultMap.get("predictions");
		if(result == null || result.hasFailed()) {
			Assert.fail("Result missing...");
		}

		/*
			Cast it to List:
		 */
		List prediction = (List) result.castResultData(
				"builtin.List", BuiltinCaster.class).getDataField();
		Instances testDataFromExecutor = resultMap.get("testset").castResultData("LabeledInstances", LabeledInstancesCaster.class).getDataField();
		double correctPredictions = 0.;
		Instances weatherTestSet = new DataSetService(datasetRef).fromIndicesLabeled(splits.get(1), -1);
		Assert.assertEquals(weatherTestSet.toString().trim(), testDataFromExecutor.toString().trim());
		for (int i = 0; i < prediction.size(); i++) {
			Instance testInstance = weatherTestSet.get(i);
			if(testInstance.classValue() == ((Number)prediction.get(i)).doubleValue()) {
				correctPredictions++;
			}
		}
		logger.info("{}/{} correct predictions.", (int) correctPredictions, prediction.size());
	}


	@Test
	public void testClassificationScikit1() {
		String composition =
				"s1 = sklearn.ensemble.RandomForestClassifier::__construct();\n" +
						"s1::train({trainset});\n" +
						"predictions = s1::predict({testset});\n";

		logger.info("Test classification with composition: \n" + composition);

		ResolvePolicy policy = new ResolvePolicy();
		policy.setPersistentServices(Arrays.asList("s1"));
		policy.setReturnFieldnames(Arrays.asList("predictions"));

		SEDEObject trainset = getDataField(weatherTrainSet);
		SEDEObject testset = getDataField(weatherTestSet);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("trainset", trainset);
		inputs.put("testset", testset);

		RunRequest runRequest = new RunRequest("scikit-classification", composition, policy, inputs);

		Map<String, Result> resultMap = coreClient.blockingRun(runRequest);
		Result result = resultMap.get("predictions");
		if(result == null || result.hasFailed()) {
			Assert.fail("Result missing...");
		}
		/*
			Cast it to List:
		 */
		List prediction = (List) result.castResultData(
				"builtin.List", BuiltinCaster.class).getDataField();
		int correctPredictions = MLDataSets.countMatchPredictions(prediction, weatherTestSet);
		logger.info("{}/{} correct predictions.", (int) correctPredictions, prediction.size());
	}

	@Test
	public void test_TensorflowNeuralNet() {
		String trainComposition =
				"s1 = tflib.NeuralNet::__construct();\n" +
						"s1::set_options({options});\n" +
						"s1::train({trainset});\n";
		String predictComposition =
						"predictions = s1::predict({testset});\n";

		logger.info("Test classification with compositions: \n {}and: \n{}", trainComposition,
				predictComposition);

		ResolvePolicy policy = new ResolvePolicy();
		policy.setPersistentServices(Arrays.asList("s1"));

		SEDEObject trainset = getDataField(weatherTrainSet);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("trainset", trainset);

		inputs.put("options", new ObjectDataField("builtin.List",
				Arrays.asList("epochs", "5000",
						"learning_rate", "0.05",
						"deviation", "0.95",
						"batch_size", "100",
						"log_device_placement", "false",
						"device", "/CPU:0")));

		RunRequest trainRequest = new RunRequest("neuralnet-train", trainComposition, policy, inputs);

		Map<String, Result> resultMap = coreClient.blockingRun(trainRequest);
		Result result = resultMap.get("s1");
		if(result == null || result.hasFailed()) {
			Assert.fail("Service missing...");
		}
		ServiceInstanceHandle neuralnetService = result.getServiceInstanceHandle();
		Assert.assertEquals("tflib.NeuralNet", neuralnetService.getClasspath());
		neuralnetService = new ServiceInstanceHandle(pyExecutorId, "tflib.NeuralNet", neuralnetService.getId());
		policy = new ResolvePolicy();
		policy.setReturnFieldnames(Arrays.asList("predictions"));

		SEDEObject testset = getDataField(weatherTestSet);

		inputs = new HashMap<>();
		inputs.put("testset", testset);
		inputs.put("s1", new ServiceInstanceField(neuralnetService));

		RunRequest predictRequest = new RunRequest("neuralnet-predict", predictComposition, policy, inputs);

		resultMap = coreClient.blockingRun(predictRequest);
		result = resultMap.get("predictions");
		if(result == null || result.hasFailed()) {
			Assert.fail("Result missing...");
		}
		/*
			Cast it to List:
		 */
		List prediction = (List) result.castResultData(
				"builtin.List", BuiltinCaster.class).getDataField();
		int correctPredictions = MLDataSets.countMatchPredictions(prediction, weatherTestSet);
		logger.info("{}/{} correct predictions.", (int) correctPredictions, prediction.size());
	}


	private static ClassesConfig getTestClassConfig() {
		ClassesConfig cc = new ClassesConfig(
				FileUtil.getPathOfResource("config/weka-ml-classifiers-classconf.json"),
				FileUtil.getPathOfResource("config/weka-ml-pp-classconf.json"));

		cc.appendConfigFromJsonStrings(FileUtil.readResourceAsString("config/sl-ml-classifiers-classconf.json"));
		return cc;
	}

	private static OnthologicalTypeConfig getTestTypeConfig() {
		OnthologicalTypeConfig conf = new OnthologicalTypeConfig();

		conf.appendConfigFromJsonStrings(
				FileUtil.readResourceAsString("config/builtin-typeconf.json"),
				FileUtil.readResourceAsString("config/weka-ml-typeconf.json"),
				FileUtil.readResourceAsString("config/sl-ml-typeconf.json"));
		return conf;
	}
	private static SEDEObject getDataField(Instances dataSet) {
		return new ObjectDataField("LabeledInstances", dataSet);
	}
}
