package Catalano.Imaging;

import java.util.List;
import java.util.Map;

public abstract class AbstractImageProcessor {

	public void configure(Map options) {

	}

	public abstract void applyInPlace(FastBitmap fb);

	public void applyToList(List<FastBitmap> fbList) {
		for(FastBitmap fb : fbList) {
			applyInPlace(fb);
		}
	}
}
