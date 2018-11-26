package Catalano.Imaging.Filters;

import Catalano.Imaging.wrappers.CannyEdgeDetector;

public class CannyEdgeDetectorFactory {
	public static CannyEdgeDetector withDefaults() {
		return new CannyEdgeDetector(20, 100, 1.4, 1);
	}

	public static CannyEdgeDetector withThresholds(int low, int high) {
		return new CannyEdgeDetector(low, high, 1.4, 1);
	}

	public static CannyEdgeDetector withThresholds_Sigma(int low, int high, double sigma) {
		return new CannyEdgeDetector(low, high, sigma, 1);
	}

	public static CannyEdgeDetector withThresholds_Sigma_Size(int low, int high, double sigma, int size) {
		return new CannyEdgeDetector(low, high, sigma, size);
	}
}
