package Catalano.Imaging.wrappers;

import Catalano.Imaging.AbstractImageProcessor;
import Catalano.Imaging.FastBitmap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CannyEdgeDetector extends AbstractImageProcessor {
	private Catalano.Imaging.Filters.CannyEdgeDetector cannyEdgeDetector;

	public CannyEdgeDetector(int a, int b, double c, int d){
		cannyEdgeDetector = new Catalano.Imaging.Filters.CannyEdgeDetector(a, b, c, d);
	}

	public CannyEdgeDetector() {
		cannyEdgeDetector = new Catalano.Imaging.Filters.CannyEdgeDetector();
	}

	private static final String[] options1 = {"lowThreshold", "highThreshold", "sigma", "size"};

	public void configure(Map options) {
		if(Arrays.stream(options1).allMatch(options::containsKey)) {
			int lowThreshold = ((Number) options.get("lowThreshold")).intValue();
			int highThreshold = ((Number) options.get("highThreshold")).intValue();
			double sigma = ((Number) options.get("sigma")).doubleValue();
			int size = ((Number) options.get("size")).intValue();
			cannyEdgeDetector = new Catalano.Imaging.Filters.CannyEdgeDetector(lowThreshold, highThreshold, sigma, size);
		} else if(!options.isEmpty()) {
			throw new IllegalArgumentException("Options not recognized : " + options.toString());
		}
	}

	public void applyInPlace(FastBitmap fb) {
		if(cannyEdgeDetector == null) {
			throw new IllegalStateException("Image Processor not initialized.");
		}
		if(!fb.isGrayscale()) {
			fb.toGrayscale();
		}
		cannyEdgeDetector.applyInPlace(fb);
	}
}
