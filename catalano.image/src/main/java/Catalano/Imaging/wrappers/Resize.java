package Catalano.Imaging.wrappers;

import Catalano.Imaging.AbstractImageProcessor;
import Catalano.Imaging.FastBitmap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Resize extends AbstractImageProcessor {
	private Catalano.Imaging.Filters.Resize resizer;
	public Resize(int a, int b, String algorithm){
		resizer = new Catalano.Imaging.Filters.Resize(a, b,
				Catalano.Imaging.Filters.Resize.Algorithm.valueOf(algorithm));
	}

	public Resize(){
		resizer = null;
	}


	private static final String[] options1 = {"width", "height", "method"};

	private static final String[] options2 = {"width", "height"};

	public void configure(Map options) {
		if(Arrays.stream(options1).allMatch(options::containsKey)) {
			int width = ((Number) options.get("width")).intValue();
			int height = ((Number) options.get("highThreshold")).intValue();
			String method = options.get("method").toString();
			resizer = new Catalano.Imaging.Filters.Resize(width, height,
					Catalano.Imaging.Filters.Resize.Algorithm.valueOf(method));
		} else if (Arrays.stream(options2).allMatch(options::containsKey)) {
			int width = ((Number) options.get("width")).intValue();
			int height = ((Number) options.get("height")).intValue();
			resizer = new Catalano.Imaging.Filters.Resize(width, height);
		} else {
			throw new IllegalArgumentException("Options not recognized : " + options.toString());
		}
	}

	public void applyInPlace(FastBitmap fb) {
		if(resizer == null) {
			throw new IllegalStateException("Image Processor not initialized.");
		}
		resizer.applyInPlace(fb);
	}

}
