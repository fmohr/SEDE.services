package Catalano.Imaging.wrappers;

import Catalano.Imaging.AbstractImageProcessor;
import Catalano.Imaging.FastBitmap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Crop extends AbstractImageProcessor {

	private Catalano.Imaging.Filters.Crop cropper;
	public Crop(int a, int b, int c, int d){
		cropper = new Catalano.Imaging.Filters.Crop(a, b, c, d);
	}

	public Crop() {
	}


	private static final String[] options1 = {"x", "y", "width", "height"};

	public void configure(Map options) {
		if(Arrays.stream(options1).allMatch(options::containsKey)) {
			int x =  ((Number) options.get("x")).intValue();
			int y =  ((Number) options.get("y")).intValue();
			int width =  ((Number) options.get("width")).intValue();
			int height =  ((Number) options.get("height")).intValue();
			cropper = new Catalano.Imaging.Filters.Crop(x, y, width, height);
		} else {
			throw new IllegalArgumentException("Options not recognized : " + options.toString());
		}
	}

	public void applyInPlace(FastBitmap fb) {
		if(cropper == null) {
			throw new IllegalStateException("Image Processor not initialized.");
		}
		cropper.ApplyInPlace(fb);
	}
}
