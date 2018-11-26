package de.upb.sede.casters;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;

import java.io.*;

public class ImageZipCaster {

	public InputStream cfs_ImageZip(InputStream is) throws IOException {
		if(is instanceof ByteArrayInputStream) {
			return is;
		} else {
			/*
			 * The stream may not be persistent:
			 */
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			IOUtils.copy(is, buffer);
			return new ByteArrayInputStream(buffer.toByteArray());
		}
	}

	public void cts_ImageZip(OutputStream os, InputStream is) throws IOException {
		IOUtils.copy(is, os);
	}
}
