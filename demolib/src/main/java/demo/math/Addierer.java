package demo.math;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import demo.types.NummerList;


public class Addierer implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(Addierer.class);
	private static final long serialVersionUID = 1L;
	final double basisZahl;

	public Addierer(int basisZahl) {
		this.basisZahl = basisZahl;
	}

	public double addier(double summand) {
		return basisZahl + summand;
	}

	public NummerList addierListe(NummerList nummerListe) {
//		try {
//			Thread.sleep(300);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		NummerList sumList = new NummerList();
		for (double summand : nummerListe) {
			sumList.add(addier(summand));
		}
		return sumList;
	}


	public static NummerList summierListe(NummerList nl1, NummerList nl2) {
//		try {
//			Thread.sleep(00);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		NummerList sumList = new NummerList();
		for (int i = 0, size = Math.min(nl1.size(), nl2.size()); i < size; i++) {
			sumList.add(nl1.get(i) + nl2.get(i));
		}
		return sumList;
	}

	public static List<Number> addierBuiltIn(List<Number> liste, double delta) {
		List<Number> addedList = new ArrayList<>();
		liste.forEach(elem -> addedList.add(elem.doubleValue() + delta));
		return addedList;
	}

	/**
	 * Method to test errors during execution.
	 */
	public static NummerList fail() {
		throw new RuntimeException("I fail because in this world it isn't worth trying not to.");
	}

	/**
	 * Method to test interruptibility during execution.
	 */
	public static void sleep() throws InterruptedException {
		Object obj = new Object();
		synchronized (obj) {
			try {
				logger.info("started sleeping.");
				obj.wait();
			} catch (InterruptedException e) {
				logger.info("interrupted sleep.");
//				throw e;
			}
		}
	}
}
