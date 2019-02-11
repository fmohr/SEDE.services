package Catalano.Imaging.wrappers;

import Catalano.Imaging.AbstractImageProcessor;
import Catalano.Imaging.FastBitmap;

import java.util.List;

public class SobelEdgeDetector extends AbstractImageProcessor {
	private Catalano.Imaging.Filters.SobelEdgeDetector sobelEdgeDetector;
	public SobelEdgeDetector(){
		sobelEdgeDetector = new Catalano.Imaging.Filters.SobelEdgeDetector();
	}

	public void applyInPlace(FastBitmap fb) {
		if(!fb.isGrayscale()) {
			fb.toGrayscale();
		}
		sobelEdgeDetector.applyInPlace(fb);
	}
}
