package demo;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.Resize;
import de.upb.sede.BuiltinCaster;
import de.upb.sede.casters.FastBitmapCaster;
import de.upb.sede.client.CoreClient;
import de.upb.sede.client.HttpCoreClient;
import de.upb.sede.config.ClassesConfig;
import de.upb.sede.config.ExecutorConfiguration;
import de.upb.sede.config.OnthologicalTypeConfig;
import de.upb.sede.core.ObjectDataField;
import de.upb.sede.core.PrimitiveDataField;
import de.upb.sede.core.SEDEObject;
import de.upb.sede.core.SemanticDataField;
import de.upb.sede.core.ServiceInstanceField;
import de.upb.sede.core.ServiceInstanceHandle;
import de.upb.sede.exec.ExecutorHttpServer;
import de.upb.sede.gateway.GatewayHttpServer;
import de.upb.sede.requests.Result;
import de.upb.sede.requests.RunRequest;
import de.upb.sede.requests.resolve.ResolvePolicy;
import de.upb.sede.util.ExecutorConfigurationCreator;
import de.upb.sede.util.FileUtil;
import de.upb.sede.util.Streams;

public class PipeLineTests {

	static CoreClient coreClient;

	static String clientAddress = "localhost";
	static int clientPort = 7000;

	static String gatewayAddress = "localhost";
	static int gatewayPort = 30370;

	static String executor1Address = "localhost";
	static int executor1Port = 9000;
	static ExecutorHttpServer executor_grayscaler;

	static String executor2Address = "localhost";
	static int executor2Port = 9001;
	static ExecutorHttpServer executor_edgedetector;

	static String executor3Address = "localhost";
	static int executor3Port = 9002;
	static ExecutorHttpServer executor_binarypattern;

	static String executor4Address = "localhost";
	static int executor4Port = 9003;
	static ExecutorHttpServer executor_weka;

	static GatewayHttpServer gateway;



	static List<FastBitmap> imageList;
	static List<String> labelList;

	static List<FastBitmap> imageList_test;


	Map<String, ServiceInstanceHandle> services = new HashMap<>();


	private final static Logger logger = LoggerFactory.getLogger(PipeLineTests.class);


	@BeforeClass
	public static void startGatway() {
		gateway = new GatewayHttpServer(gatewayPort, getTestClassConfig(), getTestTypeConfig());
	}

	@BeforeClass
	public static void startClient() {
		ExecutorConfigurationCreator creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("Client").withThreadNumberId(8);
		ExecutorConfiguration configuration = ExecutorConfiguration.parseJSON(creator.toString());
		coreClient = HttpCoreClient.createNew(configuration, clientAddress, clientPort, gatewayAddress, gatewayPort);
		/*
			Disable if you will have an executor register to the gateway:
		 */
//		coreClient.getClientExecutor().getExecutorConfiguration().getSupportedServices().addAll(
//				Arrays.asList(
//					"Catalano.Imaging.Filters.GrayScale",
//					"Catalano.Imaging.Filters.GrayScale_RGBCoeff",
//					"Catalano.Imaging.Filters.GrayScale_Lightness",
//					"Catalano.Imaging.Filters.GrayScale_Average",
//					"Catalano.Imaging.Filters.GrayScale_GeometricMean",
//					"Catalano.Imaging.Filters.GrayScale_Luminosity",
//					"Catalano.Imaging.Filters.GrayScale_MinimumDecomposition",
//					"Catalano.Imaging.Filters.GrayScale_MaximumDecomposition"
// 				));

		/*
			Disabled if you dont have dot installed.
		 */
		coreClient.writeDotGraphToDir("testrsc");
	}
	/*
		Disable if you dont want to have a local executor do the imaging:
	 */
	@BeforeClass
	public static void startupExecutors() {
		ExecutorConfigurationCreator creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("executor_grayscaler").withThreadNumberId(6);
		executor_grayscaler = new ExecutorHttpServer(ExecutorConfiguration.parseJSON(creator.toString()), executor1Address, executor1Port);
		executor_grayscaler.getBasisExecutor().getExecutorConfiguration().getSupportedServices().addAll(
				Arrays.asList(
						"Catalano.Imaging.Filters.GrayScale",
						"Catalano.Imaging.Filters.GrayScale_RGBCoeff",
						"Catalano.Imaging.Filters.GrayScale_Lightness",
						"Catalano.Imaging.Filters.GrayScale_Average",
						"Catalano.Imaging.Filters.GrayScale_GeometricMean",
						"Catalano.Imaging.Filters.GrayScale_Luminosity",
						"Catalano.Imaging.Filters.GrayScale_MinimumDecomposition",
						"Catalano.Imaging.Filters.GrayScale_MaximumDecomposition"
				));
		gateway.getBasis().register(executor_grayscaler.getBasisExecutor().registration());

		creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("executor_edgedetector").withThreadNumberId(6);
		executor_edgedetector = new ExecutorHttpServer(ExecutorConfiguration.parseJSON(creator.toString()), executor2Address, executor2Port);
		executor_edgedetector.getBasisExecutor().getExecutorConfiguration().getSupportedServices().addAll(
				Arrays.asList(
						"Catalano.Imaging.Filters.CannyEdgeDetector",
						"Catalano.Imaging.Filters.CannyEdgeDetector_Threshold",
						"Catalano.Imaging.Filters.CannyEdgeDetector_Threshold_Sigma",
						"Catalano.Imaging.Filters.CannyEdgeDetector_Threshold_Sigma_Size",

						"Catalano.Imaging.Filters.SobelEdgeDetector"
				));
		gateway.getBasis().register(executor_edgedetector.getBasisExecutor().registration());

		creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("executor_binarypattern").withThreadNumberId(6);
		executor_binarypattern = new ExecutorHttpServer(ExecutorConfiguration.parseJSON(creator.toString()), executor3Address, executor3Port);
		executor_binarypattern.getBasisExecutor().getExecutorConfiguration().getSupportedServices().addAll(
				Arrays.asList(
						"Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern"
				));
		gateway.getBasis().register(executor_binarypattern.getBasisExecutor().registration());

		creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("executor_weka").withThreadNumberId(6);
		executor_weka = new ExecutorHttpServer(ExecutorConfiguration.parseJSON(creator.toString()), executor4Address, executor4Port);
		executor_weka.getBasisExecutor().getExecutorConfiguration().getSupportedServices().addAll(
				Arrays.asList(
						"de.upb.sede.services.mls.Labeler",
						"weka.attributeSelection.AttributeSelection",
						"weka.classifiers.functions.MultilayerPerceptron"
				));
		gateway.getBasis().register(executor_weka.getBasisExecutor().registration());

	}


	@AfterClass
	public static void shutdown() {
		gateway.shutdown();
		executor_grayscaler.shutdown();
		executor_edgedetector.shutdown();
		executor_binarypattern.shutdown();
		coreClient.getClientExecutor().shutdown();
	}

	@BeforeClass
	public static void loadImages() {
		FastBitmap frog = new FastBitmap(
				FileUtil.getPathOfResource("images/red-eyed.jpg"));
		FastBitmap bear = new FastBitmap(
				FileUtil.getPathOfResource("images/bear-salmon.jpg"));
		FastBitmap goat = new FastBitmap(
				FileUtil.getPathOfResource("images/morning-exercise.jpg"));

		imageList = Arrays.asList(frog, bear, goat);
		labelList = Arrays.asList("frog", "bear", "goat");


		FastBitmap bear_test = new FastBitmap(
				FileUtil.getPathOfResource("images/bear-canada.jpg"));
		FastBitmap frog_test = new FastBitmap(
				FileUtil.getPathOfResource("images/red-eyed2.jpg"));
		imageList_test = Arrays.asList(bear_test, frog_test);

		Resize resize = new Resize(128,128);
		imageList.forEach(resize::applyInPlace);
		imageList_test.forEach(resize::applyInPlace);


	}

	@Before
	public void resetServiceMap() {
		services.clear();
	}

	@Test
	public void testImageClassification_pureJava() {
		String composition_train =
				"s1 = Catalano.Imaging.Filters.GrayScale_Luminosity::__construct();\n" +
				"i1 = s1::applyToList({i0});\n" +
				"s2 = Catalano.Imaging.Filters.SobelEdgeDetector::__construct();\n" +
				"edge_detected_images = s2::applyToList({i1});\n" +

				"s3 = Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern::__construct();\n" +
				"dataset = s3::ComputeFeatureSet({edge_detected_images});\n" +

				"de.upb.sede.services.mls.Labeler::setClassIndex({dataset, -1});\n" +
				"de.upb.sede.services.mls.Labeler::labelDataset({dataset, labels});\n" +

				"attrSelector = weka.attributeSelection.AttributeSelection::__construct({" +
										"searcher, evaluator, null, null});\n" +
				"attrSelector::train({dataset});\n" +
				"dataset_post = attrSelector::preprocess({dataset});\n" +

				"classifier = weka.classifiers.functions.MultilayerPerceptron::__construct();\n" +
				"classifier::train({dataset_post});\n";



		ResolvePolicy policy = new ResolvePolicy();

		policy.setPersistentServices(Arrays.asList("attrSelector", "classifier"));
		policy.setReturnFieldnames(Arrays.asList("dataset", "dataset_post", "edge_detected_images"));

		SEDEObject inputObject_i0 = new ObjectDataField("FastBitmap_List", imageList);
		SEDEObject inputObject_labels = new ObjectDataField("builtin.List", labelList);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("i0", inputObject_i0);
		inputs.put("labels", inputObject_labels);
		inputs.put("searcher", new PrimitiveDataField("weka.attributeSelection.Ranker"));
		inputs.put("evaluator", new PrimitiveDataField("weka.attributeSelection.InfoGainAttributeEval"));

		showImages( imageList, labelList, "Original images");

		RunRequest trainRequest = new RunRequest("pipe_train", composition_train, policy, inputs);



		coreClient.run(trainRequest, this::handleResults);
		coreClient.join("pipe_train", true);


		Assert.assertTrue(services.containsKey("classifier"));
		Assert.assertTrue(services.containsKey("attrSelector"));


		String composition_predict =
				"s1 = Catalano.Imaging.Filters.GrayScale_Luminosity::__construct();\n" +
				"i1 = s1::applyToList({i0});\n" +
				"s2 = Catalano.Imaging.Filters.SobelEdgeDetector::__construct();\n" +
				"i2 = s2::applyToList({i1});\n" +

				"s3 = Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern::__construct();\n" +
				"dataset = s3::ComputeFeatureSet({i2});\n" +

				"de.upb.sede.services.mls.Labeler::setClassIndex({dataset, -1});\n" +

				"dataset_post = attrSelector::preprocess({dataset});\n" +

				"indixed_predictions = classifier::predict({dataset_post});\n" +
				"predictions = de.upb.sede.services.mls.Labeler::classIndicesToNames({labels_original, indixed_predictions});";

		policy = new ResolvePolicy();

		policy.setServicePolicy("None");
		policy.setReturnFieldnames(Arrays.asList("dataset", "dataset_post", "i2", "predictions"));


		inputObject_i0 = new ObjectDataField("FastBitmap_List", imageList_test);

		inputs.clear();
		inputs.put("i0", inputObject_i0);
		inputs.put("labels_original", inputObject_labels);
		inputs.put("attrSelector", new ServiceInstanceField(services.get("attrSelector")));
		inputs.put("classifier", new ServiceInstanceField(services.get("classifier")));

		showImages(imageList_test, imageList_test.stream().map(fb -> "?").collect(Collectors.toList()), "To be predicted");

		RunRequest predictRequest = new RunRequest("pipe_predict", composition_predict, policy, inputs);
		Map<String, Result> predictionsResult = coreClient.blockingRun(predictRequest);

		Assert.assertTrue(predictionsResult.containsKey("predictions"));
		Assert.assertTrue(predictionsResult.containsKey("i2"));
		Assert.assertFalse(predictionsResult.get("predictions").hasFailed());
		Assert.assertFalse(predictionsResult.get("i2").hasFailed());

		List<String> predictions = (List<String>) predictionsResult.get("predictions").castResultData("builtin.List", BuiltinCaster.class).getDataField();
		List<FastBitmap> edge_detected = (List<FastBitmap>) predictionsResult.get("i2").castResultData("FastBitmap_List", FastBitmapCaster.class).getDataField();
		
		showImages(edge_detected, predictions, "Predictions");

		FileUtil.writeStringToFile("testrsc/dataset_predict_post.arff",
				Streams.InReadString(predictionsResult.get("dataset_post").getResultData().getDataField()));
		FileUtil.writeStringToFile("testrsc/dataset_predict.arff",
				Streams.InReadString(predictionsResult.get("dataset").getResultData().getDataField()));
	}
	

	@Test
	public void  testImageClassification_tf_neuralnet() throws InvocationTargetException, InterruptedException {
		/*
		 * Check if a python executor is available.
		 */
		while(gateway.getBasis().getExecutorCoord().executorsSupportingServiceClass("tflib.NeuralNet").isEmpty()) {
			Object[] options1 = { "Check again", "Cancel test"};
			JPanel panel = new JPanel();
			panel.add(new JLabel("No registered executor supports: \"tflib.NeuralNet\""));
			EventQueue.invokeAndWait(()-> {
				int result = JOptionPane.showOptionDialog(null, panel, "Test halted",
						JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE,
						null, options1, null);
				if(result == 1) {
					Assert.fail();
				}
			});
		}
		String composition_train =
				"s1 = Catalano.Imaging.Filters.GrayScale_Luminosity::__construct();\n" +
				"i1 = s1::applyToList({i0});\n" +
				"s2 = Catalano.Imaging.Filters.CannyEdgeDetector::__construct();\n" +
				"edge_detected_images = s2::applyToList({i1});\n" +

				"s3 = Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern::__construct();\n" +
				"dataset = s3::ComputeFeatureSet({edge_detected_images});\n" +

				"de.upb.sede.services.mls.Labeler::setClassIndex({dataset, -1});\n" +
				"de.upb.sede.services.mls.Labeler::labelDataset({dataset, labels});\n" +

				"attrSelector = weka.attributeSelection.AttributeSelection::__construct({" +
				"searcher, evaluator, null, null});\n" +
				"attrSelector::train({dataset});\n" +
				"dataset_post = attrSelector::preprocess({dataset});\n" +

				"classifier = tflib.NeuralNet::__construct();\n" +
				"classifier::set_options({options});\n" +
				"classifier::train({dataset_post});\n";



		ResolvePolicy policy = new ResolvePolicy();

		policy.setPersistentServices(Arrays.asList("attrSelector", "classifier"));
		policy.setReturnFieldnames(Arrays.asList("dataset", "dataset_post", "edge_detected_images"));

		SEDEObject inputObject_i0 = new ObjectDataField("FastBitmap_List", imageList);
		SEDEObject inputObject_labels = new ObjectDataField("builtin.List", labelList);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("i0", inputObject_i0);
		inputs.put("labels", inputObject_labels);
		inputs.put("searcher", new PrimitiveDataField("weka.attributeSelection.Ranker"));
		inputs.put("evaluator", new PrimitiveDataField("weka.attributeSelection.InfoGainAttributeEval"));
		inputs.put("options", new ObjectDataField("builtin.List",
				Arrays.asList("epochs", "500",
						"learning_rate", "0.05",
						"deviation", "0.95",
						"log_device_placement", "true",
						"device", "/CPU:0")));

		showImages( imageList, labelList, "Original images");

		RunRequest trainRequest = new RunRequest("pipe_train", composition_train, policy, inputs);



		coreClient.run(trainRequest, this::handleResults);
		coreClient.join("pipe_train", true);


		Assert.assertTrue(services.containsKey("classifier"));
		Assert.assertTrue(services.containsKey("attrSelector"));


		String composition_predict =
				"s1 = Catalano.Imaging.Filters.GrayScale_Luminosity::__construct();\n" +
				"i1 = s1::applyToList({i0});\n" +
				"s2 = Catalano.Imaging.Filters.CannyEdgeDetector::__construct();\n" +
				"i2 = s2::applyToList({i1});\n" +

				"s3 = Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern::__construct();\n" +
				"dataset = s3::ComputeFeatureSet({i2});\n" +

				"de.upb.sede.services.mls.Labeler::setClassIndex({dataset, -1});\n" +

				"dataset_post = attrSelector::preprocess({dataset});\n" +

				"indixed_predictions = classifier::predict({dataset_post});\n" +
				"predictions = de.upb.sede.services.mls.Labeler::classIndicesToNames({labels_original, indixed_predictions});";

		policy = new ResolvePolicy();

		policy.setServicePolicy("None");
		policy.setReturnFieldnames(Arrays.asList("dataset_post", "i2", "predictions"));


		inputObject_i0 = new ObjectDataField("FastBitmap_List", imageList_test);

		inputs.clear();
		inputs.put("i0", inputObject_i0);
		inputs.put("labels_original", inputObject_labels);
		inputs.put("attrSelector", new ServiceInstanceField(services.get("attrSelector")));
		inputs.put("classifier", new ServiceInstanceField(services.get("classifier")));

		showImages(imageList_test, imageList_test.stream().map(fb -> "?").collect(Collectors.toList()), "To be predicted");

		RunRequest predictRequest = new RunRequest("pipe_predict", composition_predict, policy, inputs);
		Map<String, Result> predictionsResult = coreClient.blockingRun(predictRequest);

		Assert.assertTrue(predictionsResult.containsKey("predictions"));
		Assert.assertTrue(predictionsResult.containsKey("i2"));
		Assert.assertFalse(predictionsResult.get("predictions").hasFailed());
		Assert.assertFalse(predictionsResult.get("i2").hasFailed());

		List<String> predictions = (List<String>) predictionsResult.get("predictions").castResultData("builtin.List", BuiltinCaster.class).getDataField();
		List<FastBitmap> edge_detected = (List<FastBitmap>) predictionsResult.get("i2").castResultData("FastBitmap_List", FastBitmapCaster.class).getDataField();

		showImages(edge_detected, predictions, "Predictions");

	}

	synchronized void handleResults(Result result) {
		if(result.hasFailed()) {
			logger.error("Failed flag for Field: '{}'.", result.getFieldname());
			return;
		}
		if(result.getResultData().isServiceInstanceHandle()) {
			services.put(result.getFieldname(), result.getServiceInstanceHandle());
		}
		else if (result.getResultData().getType().equals("dataset")){
			SEDEObject resultObj = result.getResultData();
			if (resultObj.isSemantic()) {
				String arffData = Streams.InReadString(((SemanticDataField) resultObj).getDataField());
				logger.trace("Dataset from localbinarypattern: {}", arffData);
			} else {
				logger.error("Dataset resykt not in fom of semantic data: '{}'.", result.getFieldname());
			}
		} else if(result.getResultData().getType().equals("images")){
			List<FastBitmap> processedImage = result.castResultData(
					"FastBitmap_List", FastBitmapCaster.class).getDataField();
			showImages(processedImage, labelList, result.getFieldname());
		}
	}

	private void showImages(List<FastBitmap> images, List<String> labels, String title) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		for (int i = 0; i < images.size(); i++) {
			JLabel label = new JLabel();
			label.setIcon(images.get(i).toIcon());
			label.setText(labels.get(i));
			panel.add(label);
		}
		try {
			EventQueue.invokeAndWait(() -> JOptionPane.showMessageDialog(null,panel,title,JOptionPane.PLAIN_MESSAGE));
		} catch (InterruptedException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private static ClassesConfig getTestClassConfig() {
		ClassesConfig classesConfig = new ClassesConfig();
		classesConfig.appendConfigFromJsonStrings(FileUtil.readResourceAsString("config/imaging-classconf.json"));
		classesConfig.appendConfigFromJsonStrings(FileUtil.readResourceAsString("config/weka-ml-classifiers-classconf.json"));
		classesConfig.appendConfigFromJsonStrings(FileUtil.readResourceAsString("config/weka-ml-pp-classconf.json"));
		classesConfig.appendConfigFromJsonStrings(FileUtil.readResourceAsString("config/sl-ml-classifiers-classconf.json"));
		return classesConfig;
	}

	private static OnthologicalTypeConfig getTestTypeConfig() {
		OnthologicalTypeConfig typeConfig = new OnthologicalTypeConfig();

		typeConfig.appendConfigFromJsonStrings(
				FileUtil.readResourceAsString("config/imaging-typeconf.json"),
				FileUtil.readResourceAsString("config/builtin-typeconf.json"),
				FileUtil.readResourceAsString("config/weka-ml-typeconf.json"),
				FileUtil.readResourceAsString("config/sl-ml-typeconf.json"));
		return typeConfig;
	}

}
