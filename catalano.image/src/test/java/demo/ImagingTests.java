package demo;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.ImageHistogram;
import Catalano.Statistics.Histogram;
import de.upb.sede.casters.FastBitmapCaster;
import de.upb.sede.casters.ImageHistogramCaster;
import de.upb.sede.client.CoreClient;
import de.upb.sede.client.HttpCoreClient;
import de.upb.sede.config.ClassesConfig;
import de.upb.sede.config.OnthologicalTypeConfig;
import de.upb.sede.core.ObjectDataField;
import de.upb.sede.core.SEDEObject;
import de.upb.sede.config.ExecutorConfiguration;
import de.upb.sede.exec.ExecutorHttpServer;
import de.upb.sede.gateway.GatewayHttpServer;
import de.upb.sede.requests.Result;
import de.upb.sede.requests.RunRequest;
import de.upb.sede.requests.resolve.ResolvePolicy;
import de.upb.sede.util.ExecutorConfigurationCreator;
import de.upb.sede.util.FileUtil;
import de.upb.sede.util.WebUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ImagingTests {

	static CoreClient coreClient;

	static String clientAddress = "localhost";
	static int clientPort = 7000;

	static String gatewayAddress = "localhost";
	static int gatewayPort = 30370;

	static FastBitmap frog;

	static String executor1Address = WebUtil.HostIpAddress();
	static int executorPort = 9000;
	static ExecutorHttpServer executor1;

	static GatewayHttpServer gateway;

	@BeforeClass
	public static void startClient() {

		gateway = new GatewayHttpServer(gatewayPort, getTestClassConfig(), getTestTypeConfig());
		ExecutorConfigurationCreator creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("Client");
		ExecutorConfiguration configuration = ExecutorConfiguration.parseJSON(creator.toString());
		coreClient = HttpCoreClient.createNew(configuration, clientAddress, clientPort, gatewayAddress, gatewayPort);
		/*
			Disable if you will have an executor register to the gateway:
		 */
//		coreClient.getClientExecutor().getExecutorConfiguration().getSupportedServices().addAll(
//				Arrays.asList("Catalano.Imaging.Filters.Crop",
//						"Catalano.Imaging.Filters.Resize",
//						"Catalano.Imaging.sede.CropFrom0")
//		);
		/*
			Disable if you dont want to have a local executor do the imaging:
		 */
		creator = new ExecutorConfigurationCreator();
		creator.withExecutorId("executor");
		executor1 = new ExecutorHttpServer(ExecutorConfiguration.parseJSON(creator.toString()), executor1Address, executorPort);
		executor1.getBasisExecutor().getExecutorConfiguration().getSupportedServices().addAll(
				Arrays.asList(
						"Catalano.Imaging.Filters.Crop",
						"Catalano.Imaging.Filters.Resize",
						"Catalano.Imaging.sede.CropFrom0",
						"Catalano.Imaging.Filters.CannyEdgeDetector",
						"Catalano.Imaging.Filters.SobelEdgeDetector",
						"Catalano.Imaging.Filters.CannyEdgeDetector_Threshold",
						"Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern",
						"Catalano.Imaging.Filters.GrayScale",
						"Catalano.Imaging.Filters.GrayScale_RGBCoeff",
						"Catalano.Imaging.Filters.GrayScale_Lightness",
						"Catalano.Imaging.Filters.GrayScale_Average",
						"Catalano.Imaging.Filters.GrayScale_GeometricMean",
						"Catalano.Imaging.Filters.GrayScale_Luminosity",
						"Catalano.Imaging.Filters.GrayScale_MinimumDecomposition",
						"Catalano.Imaging.Filters.GrayScale_MaximumDecomposition"
						)
		);
		System.out.println(executor1.getBasisExecutor().getExecutorConfiguration().toJsonString());
		gateway.getBasis().register(executor1.getBasisExecutor().registration());
		/*
			Disabled if you dont have dot installed.
		 */
		coreClient.writeDotGraphToDir("testrsc");

	}



	@AfterClass
	public static void shutdown() {
//		gateway.shutdown();
//		executor1.shutdown();
		coreClient.getClientExecutor().shutdown();
	}

	@BeforeClass
	public static void loadImages() {
		 frog = new FastBitmap(FileUtil.getPathOfResource("images/red-eyed.jpg"));
		 assert frog != null;
	}

	@Test
	public void testImageProcessing1() {
		String composition =
				"s1 = Catalano.Imaging.Filters.Crop::__construct({i1=5, i2=5, i3=300, i4=300});\n" +
				"s1::applyInPlace({i1=imageIn});\n" +
				"s2 = Catalano.Imaging.Filters.Resize::__construct({i1=200, i2=200});\n" +
				"imageOut = s2::applyInPlace({i1=imageIn});";

		ResolvePolicy policy = new ResolvePolicy();
		policy.setServicePolicy("None");
		policy.setReturnFieldnames(Arrays.asList("imageOut"));

		SEDEObject inputObject_fb1 = new ObjectDataField(FastBitmap.class.getName(), frog);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("imageIn", inputObject_fb1);

		JOptionPane.showMessageDialog(null, frog.toIcon(), "Original image", JOptionPane.PLAIN_MESSAGE);

		RunRequest runRequest = new RunRequest("processing1", composition, policy, inputs);

		Map<String, Result> resultMap = coreClient.blockingRun(runRequest);
		Result result = resultMap.get("imageOut");
		if(result == null || result.hasFailed()) {
			Assert.fail("Result missing...");
		}
		/*
			Cast it to bitmap:
		 */
		FastBitmap processedImage = (FastBitmap) result.castResultData(
				FastBitmap.class.getName(), FastBitmapCaster.class).getDataField();
		JOptionPane.showMessageDialog(null, processedImage.toIcon(), "Result", JOptionPane.PLAIN_MESSAGE);
	}

	@Test
	public void testImageProcessing2() {
		/*
			Tests fixed parameters:
		 */
		String composition =
				"s1  = Catalano.Imaging.sede.CropFrom0::__construct({100,100});\n" +
						"imageOut = s1::applyInPlace({i1=imageIn});";

		ResolvePolicy policy = new ResolvePolicy();
		policy.setServicePolicy("None");
		policy.setReturnFieldnames(Arrays.asList("imageOut"));

		SEDEObject inputObject_fb1 = new ObjectDataField(FastBitmap.class.getName(), frog);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("imageIn", inputObject_fb1);

		JOptionPane.showMessageDialog(null, frog.toIcon(), "Original image", JOptionPane.PLAIN_MESSAGE);

		RunRequest runRequest = new RunRequest("processing2", composition, policy, inputs);

		Map<String, Result> resultMap = coreClient.blockingRun(runRequest);
		Result result = resultMap.get("imageOut");
		if(result == null || result.hasFailed()) {
			Assert.fail("Result missing...");
		}
		/*
			Cast it to bitmap:
		 */
		FastBitmap processedImage = result.castResultData(
				FastBitmap.class.getName(), FastBitmapCaster.class).getDataField();
		JOptionPane.showMessageDialog(null, processedImage.toIcon(), "Result", JOptionPane.PLAIN_MESSAGE);
	}

	@Test
	public void testImageProcessingGrayScaleEdgeDetection() throws InvocationTargetException, InterruptedException {
		/*
			Tests fixed parameters:
		 */
		String composition =
				"s1 = Catalano.Imaging.Filters.GrayScale_Luminosity::__construct();\n" +
						"i1 = s1::applyInPlace({i0});\n" +
						"s2 = Catalano.Imaging.Filters.SobelEdgeDetector::__construct();\n" +
						"i2 = s1::applyInPlace({i1});\n" +
						"s3 = Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern::__construct();\n" +
						"histogram = s3::ComputeFeatures({i2});";

		ResolvePolicy policy = new ResolvePolicy();

		policy.setServicePolicy("None");
		policy.setReturnFieldnames(Arrays.asList("i1", "i2", "histogram"));

		SEDEObject inputObject_fb1 = new ObjectDataField(FastBitmap.class.getName(), frog);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("i0", inputObject_fb1);

		EventQueue.invokeAndWait(() -> JOptionPane.showMessageDialog(null, frog.toIcon(), "Original image", JOptionPane.PLAIN_MESSAGE));

		RunRequest runRequest = new RunRequest("processing3", composition, policy, inputs);

		Map<String, ImageHistogram> resultMap = new HashMap<>();

		coreClient.run(runRequest, result -> {
			if(result.getFieldname().equals("histogram")) {
				ImageHistogram histogram = result.
						castResultData(ImageHistogram.class.getName(), ImageHistogramCaster.class).getDataField();
				resultMap.put(result.getFieldname(), histogram);
			} else {
				FastBitmap processedImage = result.castResultData(
						FastBitmap.class.getName(), FastBitmapCaster.class).getDataField();
				try {
					EventQueue.invokeAndWait(() -> {
								JOptionPane.showMessageDialog(null,
										processedImage.toIcon(),  "Image " + result.getFieldname(), JOptionPane.PLAIN_MESSAGE);
							}
					);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		});
		coreClient.join("processing3", true);
		ImageHistogram histogram = resultMap.get("histogram");
		System.out.println("Histogram: " + Arrays.toString(histogram.getValues()));
		System.out.println("Mean: " + histogram.getMean());

	}
	@Test
	public void testImageProcessing_ImageSetToDataset() throws InvocationTargetException, InterruptedException {
		/*
			Tests fixed parameters:
		 */
		String composition =
				"s1 = Catalano.Imaging.Filters.GrayScale_Luminosity::__construct();\n" +
						"i1 = s1::applyInPlace({i0});\n" +
						"s2 = Catalano.Imaging.Filters.SobelEdgeDetector::__construct();\n" +
						"i2 = s1::applyInPlace({i1});\n" +
						"s3 = Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern::__construct();\n" +
						"histogram = s3::ComputeFeatures({i2});";

		ResolvePolicy policy = new ResolvePolicy();

		policy.setServicePolicy("None");
		policy.setReturnFieldnames(Arrays.asList("i1", "i2", "histogram"));

		SEDEObject inputObject_fb1 = new ObjectDataField(FastBitmap.class.getName(), frog);

		Map<String, SEDEObject> inputs = new HashMap<>();
		inputs.put("i0", inputObject_fb1);

		EventQueue.invokeAndWait(() -> JOptionPane.showMessageDialog(null, frog.toIcon(), "Original image", JOptionPane.PLAIN_MESSAGE));

		RunRequest runRequest = new RunRequest("processing3", composition, policy, inputs);

		Map<String, ImageHistogram> resultMap = new HashMap<>();

		coreClient.run(runRequest, result -> {
			if(result.getFieldname().equals("histogram")) {
				ImageHistogram histogram = result.
						castResultData(ImageHistogram.class.getName(), ImageHistogramCaster.class).getDataField();
				resultMap.put(result.getFieldname(), histogram);
			} else {
				FastBitmap processedImage = result.castResultData(
						FastBitmap.class.getName(), FastBitmapCaster.class).getDataField();
				try {
					EventQueue.invokeAndWait(() -> {
								JOptionPane.showMessageDialog(null,
										processedImage.toIcon(),  "Image " + result.getFieldname(), JOptionPane.PLAIN_MESSAGE);
							}
					);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		});
		coreClient.join("processing3", true);
		ImageHistogram histogram = resultMap.get("histogram");
		System.out.println("Histogram: " + Arrays.toString(histogram.getValues()));
		System.out.println("Mean: " + histogram.getMean());

	}


	private static ClassesConfig getTestClassConfig() {
		ClassesConfig classesConfig = new ClassesConfig();
		classesConfig.appendConfigFromJsonStrings(FileUtil.readResourceAsString("config/imaging-classconf.json"));
		return classesConfig;
	}

	private static OnthologicalTypeConfig getTestTypeConfig() {
		OnthologicalTypeConfig typeConfig = new OnthologicalTypeConfig();
		typeConfig.appendConfigFromJsonStrings(FileUtil.readResourceAsString("config/imaging-typeconf.json"));
		return typeConfig;
	}
}
