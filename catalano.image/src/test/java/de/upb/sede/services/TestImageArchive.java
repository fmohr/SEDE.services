package de.upb.sede.services;

import de.upb.sede.util.FileUtil;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipInputStream;
import static org.junit.Assert.*;

public class TestImageArchive {
	@Test
	public void test() throws IOException {
		String path = FileUtil.getPathOfResource("ImageArchive.zip");
		try(FileInputStream fis = new FileInputStream(path)) {
			ImageArchive ia = new ImageArchive(fis, true, true);
			assertEquals(Arrays.asList("cat", "dog", "dog", "cat", "dog", "cat", "dog", "cat", "cat", "dog"), ia.getLabels());
		}
	}
}
