package de.upb.sede.services;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.wrappers.Center;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.slf4j.LoggerFactory.getLogger;

public class ImageArchive {

	private final static Logger logger = getLogger(ImageArchive.class);

	private static final String FILENAME_LABELS = "labels.txt";


	private final boolean cropZipImages;

	private final List<String> labels = new ArrayList<>();

	private final List<FastBitmap> 	labeledImages = new ArrayList<>();
	private final List<String> 		labeledImagesFileNames = new ArrayList<>();

	private final List<FastBitmap> 	unlabeledImages = new ArrayList<>();
	private final List<String> 		unlabeledImagesFileNames = new ArrayList<>();

	public ImageArchive(InputStream inputStream, final boolean cropImages, final boolean predictionIncludesAllImages) throws IOException {
		Objects.requireNonNull(inputStream);
		ZipInputStream zipInputStream = new ZipInputStream(inputStream);
		this.cropZipImages = cropImages;
		ZipEntry entry;
		// while there are entries I process them
		Map<String, String> extractedLabels = new HashMap<>();
		Map<String, FastBitmap> extractedImages = new HashMap<>();
		/*
		 * Read zip input stream content:
		 * 	Parse 'labels.txt' into the labels map.
		 * 	(Try to) parse the other entries as images and put them into the images map.
		 *
		 */
		while ((entry = zipInputStream.getNextEntry()) != null) {
			String fileName = entry.getName();
			if (fileName.equalsIgnoreCase(FILENAME_LABELS)) {
				try {
					extractedLabels = parseLabels(zipInputStream);
				} catch (Exception ex) {
					logger.error("Error loading labels {}, exception message: {}", entry.getName(), ex.getMessage());
				}
			} else {
				try {
					FastBitmap image = parseImage(zipInputStream);
					extractedImages.put(entry.getName(), image);
				} catch (Exception ex) {
					logger.error("Error loading image {}, exception message: {}", entry.getName(), ex.getMessage());
				}
			}

		}
		/*
		 * Remove every label entry which doesn't have a image loaded:
		 */
		List<String> labelsWithoutImages = new ArrayList<>();
		for(String fileName : extractedLabels.keySet()) {
			FastBitmap fb = extractedImages.get(fileName);
			if(fb == null) {
				labelsWithoutImages.add(fileName);
			} else if(fb.getSize() == 0|| fb.getHeight() == 0 || fb.getWidth() == 0) {
				labelsWithoutImages.add(fileName);
			}
		}
		if(!labelsWithoutImages.isEmpty()) {
			logger.warn("The following labels are removed as there are no corresponding images found for them: {}",
					labelsWithoutImages.toString());
			labelsWithoutImages.forEach(extractedLabels::remove);
			labelsWithoutImages.forEach(extractedImages::remove);
		}

		/*
		 * Add labeled images into the 3 lists:
		 */
		for(String fileName : extractedLabels.keySet()) {
			FastBitmap image = extractedImages.remove(fileName);
			Objects.requireNonNull(image);
			addLabeledImage(fileName, extractedLabels.get(fileName), image);
			if(predictionIncludesAllImages) {
				addUnLabeledImage(fileName, image);
			}
		}
		/*
		 * The rest of the images are put into the unlabaledImages list:
		 */
		for(String fileName : extractedImages.keySet()) {
			addUnLabeledImage(fileName, extractedImages.get(fileName));
		}
	}

	private void addLabeledImage(String fileName, String label, FastBitmap image) {
		labels.add(label);
		labeledImages.add(image);
		labeledImagesFileNames.add(fileName);
	}

	private void addUnLabeledImage(String fileName, FastBitmap image) {
		unlabeledImages.add(image);
		unlabeledImagesFileNames.add(fileName);
	}

	private Map<String, String> parseLabels(final ZipInputStream inputStream) throws IOException {
		Objects.requireNonNull(inputStream);
		List<String> lines = Arrays.asList(IOUtils.toString(inputStream, Charset.defaultCharset()).split("\n"));
		Map<String, String> labels = new HashMap<>();
		lines.forEach(l -> {
			String[] parts = l.split(",");
			if (parts.length == 2) {
				String fileName = parts[0].trim();
				String label = parts[1].trim();
				if(!label.isEmpty()) {
					labels.put(fileName, label);
				}
			}
		});
		return labels;
	}


	private FastBitmap parseImage(final ZipInputStream inputStream) throws IOException {
		Objects.requireNonNull(inputStream);
		/* read fastbitmap over tmp file */
		InputStream is = inputStream;
		BufferedImage image =  ImageIO.read(is);
		FastBitmap fb = new FastBitmap(image);
		if(cropZipImages) {
			new Center(256).applyInPlace(fb);
		}
		return fb;
	}


	public List<String> getLabels() {
		return labels;
	}

	public List<FastBitmap> getLabeledImages() {
		return labeledImages;
	}

	public List<String> getLabeledImagesFileNames() {
		return labeledImagesFileNames;
	}

	public List<FastBitmap> getUnlabeledImages() {
		return unlabeledImages;
	}

	public List<FastBitmap> getUnlabeledImagesCopy() {
		return unlabeledImages.stream().map(FastBitmap::new).collect(Collectors.toList());
	}

	public List<String> getUnlabeledImagesFileNames() {
		return unlabeledImagesFileNames;
	}
}
