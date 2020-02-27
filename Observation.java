/*
 * Kevin Lundeen
 * Fall 2018, CPSC 5600, Seattle University
 * This is free and unencumbered software released into the public domain.
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents an observation from our detection device. When a location on the
 * sensor triggers, the time and the location of the detected event are recorded
 * in one of these Observation objects.
 */
public class Observation implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final long EOF = Long.MAX_VALUE; // our convention to mark EOF with a special object

	public long time; // number of milliseconds since turning on the detector device
	public double x, y; // location of the detected event on the detection grid

	public Observation(long time, double x, double y) {
		this.time = time;
		this.x = x;
		this.y = y;
	}

	public Observation() {
		this.time = EOF;
		this.x = this.y = 0.0;
	}

	public boolean isEOF() {
		return time == EOF;
	}

	public String toString() {
		// return "Observation(" + time + ", " + x + ", " + y + ")";
		return String.format("(%d,%.2f,%.2f)", time, x, y);
	}

	public static void toFile(List<Observation> observations, String filename)
			throws FileNotFoundException, IOException {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
		for (Observation obs : observations)
			out.writeObject(obs);
		out.writeObject(new Observation()); // to mark EOF
		out.close();
	}

	public static Observation[] fromFile(String filename) throws ClassNotFoundException, IOException {
		List<Observation> observations = new ArrayList<Observation>();
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
		observations.add((Observation) in.readObject());
		while (!observations.get(observations.size() - 1).isEOF())
			observations.add((Observation) in.readObject());
		in.close();
		observations.remove(observations.size() - 1);
		Observation[] a = new Observation[observations.size()];
		observations.toArray(a);
		return a;
	}

	/**
	 * Example with serialization of a series of Observation to a local file.
	 * 
	 * @param args
	 */
//	public static void main(String[] args) {
//		final String FILENAME = "observation_gaussian.dat";
//		Random r = new Random();
//		try {
//			List<Observation> observations = new ArrayList<Observation>();
//			for (long t = 0; t < 1_000; t++) {
//				double x = r.nextGaussian() * 0.33;
//				double y = r.nextGaussian() * 0.33;
//				if (!(x < -1.0 || x > 1.0 || y < -1.0 || y > 1.0))
//					observations.add(new Observation(t, x, y));
//			}
//			toFile(observations, FILENAME);
//		} catch (IOException e) {
//			System.out.println("writing to " + FILENAME + "failed: " + e);
//			e.printStackTrace();
//			System.exit(1);
//		}
//
//		try {
//			Observation[] observations = fromFile(FILENAME);
//			int count = 0;
//			for (Observation obs : observations)
//				System.out.println(++count + ": " + obs);
//		} catch (IOException | ClassNotFoundException e) {
//			System.out.println("reading from " + FILENAME + "failed: " + e);
//			e.printStackTrace();
//			System.exit(1);
//		}
//	}

	public static void main(String[] args) {
		final String FILENAME = "observation_grant.dat";
		Random r = new Random();
		try {
			List<Observation> observations = new ArrayList<Observation>();
			double x = -0.9;
			double y = -0.9;
			for (long t = 0; t < 1_000; t++) {
				if (t % 100 == 0) {
					observations.add(new Observation(t, x, y));
					x += 0.1;
					y += 0.1;
				}
			}
			toFile(observations, FILENAME);
		} catch (IOException e) {
			System.out.println("writing to " + FILENAME + "failed: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		try {
			Observation[] observations = fromFile(FILENAME);
			int count = 0;
			for (Observation obs : observations)
				System.out.println(++count + ": " + obs);
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("reading from " + FILENAME + "failed: " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

}
