package de.upb.sede.casters;

import Catalano.Imaging.FastBitmap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides casting methods for: FastBitmap <-> image and FastBitmap_List <-> images
 */
public class FastBitmapCaster {

	/**
	 * Given a map in form of the json representation of image, this method constructs a a FastBitmap object.
	 */
	private FastBitmap jsonToFastBitmap(Map<String, Object> jsonObject) {
		int rows = ((Number) jsonObject.get("rows")).intValue();
		int columns = ((Number) jsonObject.get("columns")).intValue();
		String imagetype = jsonObject.get("imagetype").toString();
		List<Number> encodedPixels = (List<Number>) jsonObject.get("pixels");
		FastBitmap fb;
		if(imagetype.equals("Grayscale")) {
			fb = new FastBitmap(columns, rows, FastBitmap.ColorSpace.Grayscale);
			int pixelOffset = 0;
			for(Number pixel : encodedPixels) {
				fb.setGray(pixelOffset, pixel.intValue());
				pixelOffset++;
			}
		}
		else if(imagetype.equals("ARGB")) {
			fb = new FastBitmap(columns, rows, FastBitmap.ColorSpace.ARGB);
			int pixelOffset = 0;
			Iterator<Number> colorChannelIterator = encodedPixels.iterator();
			while(colorChannelIterator.hasNext()) {
				int alpha = colorChannelIterator.next().intValue();
				int red = colorChannelIterator.next().intValue();
				int green = colorChannelIterator.next().intValue();
				int blue = colorChannelIterator.next().intValue();
				fb.setARGB(pixelOffset, alpha, red, green, blue);
				pixelOffset++;
			}
		}
		else if(imagetype.equals("RGB")) {
			fb = new FastBitmap(columns, rows, FastBitmap.ColorSpace.RGB);
			int pixelOffset = 0;
			Iterator<Number> colorChannelIterator = encodedPixels.iterator();
			while(colorChannelIterator.hasNext()) {
				int red = colorChannelIterator.next().intValue();
				int green = colorChannelIterator.next().intValue();
				int blue = colorChannelIterator.next().intValue();
				fb.setRGB(pixelOffset, red, green, blue);
				pixelOffset++;
			}

		} else{
			throw new RuntimeException("Image type not supported: " + imagetype);
		}
		return fb;
	}

	/**
	 * Translates the given FastBitmap object to its json representation.
	 */
	private Map<String, Object> fastBitmapToJson(FastBitmap fb) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("rows", fb.getHeight());
		jsonObject.put("columns", fb.getWidth());
		String imagetype;
		List<Number> imageEncoding;
		if(fb.isGrayscale()) {
			imagetype = "Grayscale";
			byte[] pixelArr = fb.getGrayData();
			imageEncoding = new ArrayList<>(pixelArr.length * 1);
			for (int index = 0, size = pixelArr.length;
				 	index < size; index++) {
				imageEncoding.add(pixelArr[index] & 255);
			}

		} else if(fb.isARGB()) {
			imagetype = "ARGB";
			int[] pixelArr = fb.getRGBData(); // 'getRGBData' is correct, internally its one array for both argb and rgb
			imageEncoding = new ArrayList<>(pixelArr.length * 4);
			for (int index = 0, size = pixelArr.length;
				 	index < size; index++) {
				int argb = pixelArr[index];
				imageEncoding.add(argb  >> 24 & 255); // alpha
				imageEncoding.add(argb  >> 16 & 255); // red
				imageEncoding.add(argb  >>  8 & 255); // green
				imageEncoding.add(argb  	  & 255); // blue
			}
		} else {
			imagetype = "RGB";
			int[] pixelArr = fb.getRGBData();
			imageEncoding = new ArrayList<>(pixelArr.length * 3);
			for (int index = 0, size = pixelArr.length;
				 index < size; index++) {
				int rgb = pixelArr[index];
				imageEncoding.add(rgb  >> 16 & 255); // red
				imageEncoding.add(rgb  >>  8 & 255); // green
				imageEncoding.add(rgb  	  	 & 255); // blue
			}
		}
		jsonObject.put("imagetype", imagetype);
		jsonObject.put("pixels", imageEncoding);
		return jsonObject;
	}

	/**
	 * Casts from the semantic representation 'image' to FastBitMap object.
	 * The data in form of semantic type 'image' is taken from the provided inputstream.
	 */
	public FastBitmap cfs_FastBitmap(InputStream is) throws ParseException, IOException {
		JSONParser parser = new JSONParser();
		Reader reader = new InputStreamReader(is);
		JSONObject jsonObject = (JSONObject) parser.parse(reader);
		return jsonToFastBitmap(jsonObject);
	}


	/**
	 * Casts from the semantic representation 'images' to List of FastBitMap objects.
	 * The data in form of semantic type 'images' is taken from the provided inputstream.
	 */
	public List<FastBitmap> cfs_FastBitmap_List(InputStream is) throws ParseException, IOException {
		JSONParser parser = new JSONParser();
		Reader reader = new InputStreamReader(is);
		List<JSONObject> encodedImageList = (JSONArray) parser.parse(reader);
		List<FastBitmap> bitmapList = new ArrayList<>(encodedImageList.size());
		for(JSONObject encodedImage : encodedImageList) {
			bitmapList.add(jsonToFastBitmap(encodedImage));
		}
		return bitmapList;
	}

	/**
	 * Casts a FastBitMap object to the semantic representation 'image' and writes the data into the provided stream.
	 */
	public void cts_FastBitmap(OutputStream os, FastBitmap fb) throws IOException {
		JSONObject encodedImage = (JSONObject) fastBitmapToJson(fb);
		OutputStreamWriter writer = new OutputStreamWriter(os);
		encodedImage.writeJSONString(writer);
		writer.flush();
	}


	/**
	 * Casts a list of FastBitMap objects to the semantic representation 'images' and writes the data into the provided stream.
	 */
	public void cts_FastBitmap_List(OutputStream os, List<FastBitmap> images) throws IOException {
		ArrayList<JSONObject> encodedImageList = new ArrayList<JSONObject>(images.size());
		for(FastBitmap image : images) {
			encodedImageList.add((JSONObject) fastBitmapToJson(image));
		}
		OutputStreamWriter writer = new OutputStreamWriter(os);
		JSONArray.writeJSONString(encodedImageList, writer);
		writer.flush();
	}
}
