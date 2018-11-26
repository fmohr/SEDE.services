package Catalano.Imaging.wrappers;

import Catalano.Imaging.AbstractImageProcessor;
import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.Filters.Crop;
import Catalano.Imaging.Filters.Resize;

public class Center extends AbstractImageProcessor {
	private int size;
	public Center() {
		this(128);
	}
	public Center(int size) {
		this.size = size;
	}

	@Override
	public void applyInPlace(FastBitmap fb) {
		double width = fb.getWidth(); // 10
		double height = fb.getHeight(); // 5
		double size = this.size; //2
		int newWidth, newHeight;
		int offset_x, offset_y;
		if(width > height) {
			newWidth = (int) Math.ceil(((size / height) * width)); // 2/5 * 10 = 4
			newHeight = (int) size; // 2
			offset_x = (int) Math.ceil((newWidth - size) * 0.5); // (4 - 2) * 0.5 = 1
			offset_y = 0; //0
		} else {
			newHeight = (int) ((size / width) * height);
			newWidth = (int) size;
			offset_x = 0;
			offset_y = (int) Math.ceil((newHeight - size) * 0.5);
		}
		Resize resize = new Resize(newWidth, newHeight);
		resize.applyInPlace(fb);
		try {
			Crop crop = new Crop(offset_y, offset_x, this.size, this.size);
			crop.ApplyInPlace(fb);
		}catch (IllegalArgumentException sizeTooBig) {
			System.out.println("Couldnt crop out the center: image [" + newWidth + ", " + newHeight + "]." +
					" Crop = [" + offset_y + "-" + this.size + ", " + offset_x + "-" + this.size + "]" );
			new Resize(this.size, this.size).applyInPlace(fb);
		}
	}
}
