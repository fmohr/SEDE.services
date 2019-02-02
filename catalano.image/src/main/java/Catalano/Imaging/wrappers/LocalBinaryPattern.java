package Catalano.Imaging.wrappers;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.ImageHistogram;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalBinaryPattern {
	private Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern localBinaryPattern;
	public LocalBinaryPattern() {
		localBinaryPattern = new Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern();
	}

	public ImageHistogram ComputeFeatures(FastBitmap fastBitmap) {
		if(!fastBitmap.isGrayscale()) {
			fastBitmap.toGrayscale();
		}
		return localBinaryPattern.ComputeFeatures(fastBitmap);
	}

	public List<ImageHistogram> ComputeFeatureSet(List<FastBitmap> fbList) {
		return fbList.stream().map(this::ComputeFeatures).collect(Collectors.toList());
	}

	public void setOptions(Map m) {

	}
}
