package Catalano.Imaging.Filters;

import Catalano.Imaging.wrappers.GrayScaleByAlgorithm;

public class GrayScaleFactory {

	public static GrayScaleByAlgorithm withMethodname(String methodname) {
		return new GrayScaleByAlgorithm(methodname);
	}

	public static Catalano.Imaging.wrappers.Grayscale withRGB(double r, double g, double b) {
		return new Catalano.Imaging.wrappers.Grayscale(r, g, b);
	}

}
