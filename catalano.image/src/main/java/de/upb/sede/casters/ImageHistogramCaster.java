package de.upb.sede.casters;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Texture.BinaryPattern.LocalBinaryPattern;
import Catalano.Imaging.Tools.ImageHistogram;
import Catalano.Statistics.Histogram;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ImageHistogramCaster {
	/**
	 * Casts from the semantic representation 'Arr' which is a list of numbers, to ImageHistogram object.
	 * The data in form of semantic type 'Arr' is taken from the provided inputstream.
	 */
	public ImageHistogram cfs_ImageHistogram(InputStream is) throws ParseException, IOException {
		JSONParser parser = new JSONParser();
		Reader reader = new InputStreamReader(is);
		List<Number> arr = (JSONArray) parser.parse(reader);
		int[] values = new int[arr.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = arr.get(i).intValue();
		}
		return new ImageHistogram(values);
	}

	/**
	 * Casts an ImageHistogram object to the semantic representation 'Arr' which is a list of numbers and writes the data into the provided stream.
	 */
	public void cts_ImageHistogram(OutputStream os, ImageHistogram ih) throws IOException {
		/*
		 * copy values to a list:
		 */
		int[] values = ih.getValues();
		List<Integer>arr =new ArrayList<>(values.length);
		for (int i = 0; i < values.length; i++) {
			arr.add(values[i]);
		}

		/*
		 * write list to out:
		 */
		OutputStreamWriter writer = new OutputStreamWriter(os);
		JSONArray.writeJSONString(arr, writer);
		writer.flush();
	}

	public void cts_ImageHistogram_List(OutputStream os, List<ImageHistogram> histogramList) throws IOException {
		Writer output = new OutputStreamWriter(os);
		if(histogramList.isEmpty()) {
			throw new RuntimeException("Cannot cast an empty histogram list to a dataset.");
		}

		output.append("@relation").append(" ").append("\"image_histograms\"").append("\n\n");

//		output.append("@ATTRIBUTE mean NUMERIC").append("\n");
//		output.append("@ATTRIBUTE stdDev NUMERIC").append("\n");
//		output.append("@ATTRIBUTE entropy NUMERIC").append("\n");
//		output.append("@ATTRIBUTE kurtosis NUMERIC").append("\n");
//		output.append("@ATTRIBUTE skewness NUMERIC").append("\n");
//		output.append("@ATTRIBUTE median NUMERIC").append("\n");
//		output.append("@ATTRIBUTE mode NUMERIC").append("\n");
//		output.append("@ATTRIBUTE min NUMERIC").append("\n");
//		output.append("@ATTRIBUTE max NUMERIC").append("\n");
//		output.append("@ATTRIBUTE total NUMERIC").append("\n");

		ImageHistogram first = histogramList.get(0);
		for (int i = 0; i < first.getValues().length; i++) {
			output.append("@ATTRIBUTE v" + i + " NUMERIC").append("\n");
		}

		output.append("@ATTRIBUTE class STRING").append("\n");

		output.append("\n").append("@data").append("\n");
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
		otherSymbols.setDecimalSeparator('.');
		DecimalFormat df3 = new DecimalFormat("0.###", otherSymbols);
		for(ImageHistogram histogram : histogramList) {
			StringBuilder dataPoint = new StringBuilder();
//			dataPoint.append(df3.format(histogram.getMean()))		.append(",");
//			dataPoint.append(df3.format(histogram.getStdDev()))		.append(",");
//			dataPoint.append(df3.format(histogram.getEntropy()))	.append(",");
//			dataPoint.append(df3.format(histogram.getKurtosis()))	.append(",");
//			dataPoint.append(df3.format(histogram.getSkewness()))	.append(",");
//			dataPoint.append(df3.format(histogram.getMedian()))		.append(",");
//		dataPoint.append(df3.format(histogram.getMode()))			.append(",");
//			dataPoint.append(df3.format(histogram.getMin()))		.append(",");
//			dataPoint.append(df3.format(histogram.getMax())	)		.append(",");
//			dataPoint.append(df3.format(histogram.getTotal()))		.append(",");
			int[] values = histogram.getValues();
			for (int i = 0; i < values.length; i++) {
				dataPoint.append(values[i]).append(",");
			}
			dataPoint.append("?\n"); // unlabeled
			output.write(dataPoint.toString());
		}
		output.flush();
	}

	public List<ImageHistogram>  cfs_ImageHistogram_List(InputStream inputStream) throws IOException {
		// TODO implment this only if it is necessary to cast back to histograms.
		throw new RuntimeException("Not implementation provided to convert a dataset back to histogram list.");
	}

}
