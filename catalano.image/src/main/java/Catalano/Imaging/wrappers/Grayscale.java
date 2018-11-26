package Catalano.Imaging.wrappers;


import Catalano.Imaging.AbstractImageProcessor;
import Catalano.Imaging.FastBitmap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Grayscale extends AbstractImageProcessor {

	private Catalano.Imaging.Filters.Grayscale grayscale;

	public Grayscale(double r, double g, double b) {
		grayscale = new Catalano.Imaging.Filters.Grayscale(r, g, b);
	}

	public Grayscale() {
		grayscale = new Catalano.Imaging.Filters.Grayscale();
	}

	private static final String[] 	rgbOptions = {"red", "green", "blue"},
									methodOptions = {"method"};

	public void configure(Map options) {
		if(Arrays.stream(rgbOptions).allMatch(options::containsKey)){
			// rgb
			int red =  ((Number) options.get("red")).intValue();
			int green =  ((Number) options.get("green")).intValue();
			int blue =  ((Number) options.get("blue")).intValue();
			grayscale = new Catalano.Imaging.Filters.Grayscale(red, green, blue);
		} else if(Arrays.stream(methodOptions).allMatch(options::containsKey)){
			String method =  options.get("method").toString();
			grayscale = new Catalano.Imaging.Filters.Grayscale(Catalano.Imaging.Filters.Grayscale.Algorithm.valueOf(method));
		} else {
			throw new IllegalArgumentException("Options not recognized : " + options.toString());
		}
	}


	public void applyInPlace(FastBitmap fb) {
		if(grayscale == null) {
			throw new IllegalStateException("Image Processor not initialized.");
		}
		grayscale.applyInPlace(fb);
	}
}
