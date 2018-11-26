package de.upb.sede.services;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Tools.ImageHistogram;
import Catalano.Imaging.wrappers.Center;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Image2Feature  {

	public ImageHistogram ComputeFeatures(FastBitmap fastBitmap) {
		if(fastBitmap.getSize() > 32 || fastBitmap.getWidth() > 32) {
			new Center(32).applyInPlace(fastBitmap);
		}
		return new ImageHistogram(fastBitmap.getRGBData());
	}

	public List<ImageHistogram> ComputeFeatureSet(List<FastBitmap> fbList) {
		return fbList.stream().map(this::ComputeFeatures).collect(Collectors.toList());
	}

	public void setOptions(Map m) {

	}

}
