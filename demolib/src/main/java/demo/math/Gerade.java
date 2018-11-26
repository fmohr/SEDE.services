package demo.math;

import demo.types.Punkt;

import java.io.Serializable;

public class Gerade implements Serializable {
	double achsenabschnitt, steigung;

	public Gerade(double achsenabschnitt, double steigung) {
		this.achsenabschnitt = achsenabschnitt;
		this.steigung = steigung;
	}

	public static Gerade randomGerade(){
		return new Gerade(Math.random(), Math.random());
	}

	public boolean liegtAufGerade(Punkt p) {
		return calc(p.x).y == p.y;
	}

	public Punkt calc(double stelle) {
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		return new Punkt(stelle, steigung * stelle + achsenabschnitt);
	}

	public Punkt achsenabschnitt() {
		return new Punkt(0, achsenabschnitt);
	}
	
	public Punkt nullstelle() {
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		return new Punkt(-achsenabschnitt/steigung, 0);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Gerade)) return false;

		Gerade gerade = (Gerade) o;

		if (Double.compare(gerade.achsenabschnitt, achsenabschnitt) != 0) return false;
		return Double.compare(gerade.steigung, steigung) == 0;
	}
	
	public boolean liagtAufGerade(Punkt p){
		return Math.abs(calc(p.x).y - p.y) < 1.;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(achsenabschnitt);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(steigung);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
