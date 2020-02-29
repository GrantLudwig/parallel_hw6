/*
 * Kevin Lundeen
 * Fall 2018, CPSC 5600, Seattle University
 * This is free and unencumbered software released into the public domain.
 */

/*
 * Edited by Grant Ludwig to create different sets of data
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
	public static void main(String[] args) {
		final String FILENAME = "observation_snake.dat";
		final int ARRAY_SIZE = 5;
		Random r = new Random();
		try {
			List<Observation> observations = new ArrayList<Observation>();
			double[] xs = new double[ARRAY_SIZE];
			double[] ys = new double[ARRAY_SIZE];
			double[] xs2 = new double[ARRAY_SIZE];
			double[] ys2 = new double[ARRAY_SIZE];
			double[] inner_xs = new double[ARRAY_SIZE];
			double[] inner_ys = new double[ARRAY_SIZE];
			double[] inner_xs2 = new double[ARRAY_SIZE];
			double[] inner_ys2 = new double[ARRAY_SIZE];
			for (int i = 0; i < ARRAY_SIZE; i++) {
				// outer
				xs[i] = -0.95;
				ys[i] = -0.95;
				// outer 2
				xs2[i] = 0.95;
				ys2[i] = 0.95;
				// inner
				inner_xs[i] = 0.55;
				inner_ys[i] = -0.55;
				// inner 2
				inner_xs2[i] = -0.55;
				inner_ys2[i] = 0.55;
			}
			int outerCount = 0;
			int outerCount2 = 2;
			int innerCount = 1;
			int innerCount2 = 3;
			for (long t = 0; t < 1_000; t++) {
				for (int i = 0; i < ARRAY_SIZE; i++) {
					observations.add(new Observation(t, xs[i], ys[i])); // outer
					observations.add(new Observation(t, xs2[i], ys2[i])); // outer 2
					observations.add(new Observation(t, inner_xs[i], inner_ys[i])); // inner
					observations.add(new Observation(t, inner_xs2[i], inner_ys2[i])); // inner 2
				}
				for (int i = 0; i < ARRAY_SIZE - 1; i++) {
					// outer
					xs[i] = xs[i+1];
					ys[i] = ys[i+1];
					// outer 2
					xs2[i] = xs2[i+1];
					ys2[i] = ys2[i+1];

					// innner
					inner_xs[i] = inner_xs[i+1];
					inner_ys[i] = inner_ys[i+1];
					// inner 2
					inner_xs2[i] = inner_xs2[i+1];
					inner_ys2[i] = inner_ys2[i+1];
				}
				// Outer Display
				if (outerCount == 0) {
					xs[ARRAY_SIZE - 1] += 0.1;
					if (xs[ARRAY_SIZE - 1] >= 0.95)
						outerCount = 1;
				}
				else if (outerCount == 1) {
					ys[ARRAY_SIZE - 1] += 0.1;
					if (ys[ARRAY_SIZE - 1] >= 0.95)
						outerCount = 2;
				}
				else if (outerCount == 2) {
					xs[ARRAY_SIZE - 1] -= 0.1;
					if (xs[ARRAY_SIZE - 1] <= -0.95)
						outerCount = 3;
				}
				else {
					ys[ARRAY_SIZE - 1] -= 0.1;
					if (ys[ARRAY_SIZE - 1] <= -0.95)
						outerCount = 0;
				}

				// Outer2 Display
				if (outerCount2 == 0) {
					xs2[ARRAY_SIZE - 1] += 0.1;
					if (xs2[ARRAY_SIZE - 1] >= 0.95)
						outerCount2 = 1;
				}
				else if (outerCount2 == 1) {
					ys2[ARRAY_SIZE - 1] += 0.1;
					if (ys2[ARRAY_SIZE - 1] >= 0.95)
						outerCount2 = 2;
				}
				else if (outerCount2 == 2) {
					xs2[ARRAY_SIZE - 1] -= 0.1;
					if (xs2[ARRAY_SIZE - 1] <= -0.95)
						outerCount2 = 3;
				}
				else {
					ys2[ARRAY_SIZE - 1] -= 0.1;
					if (ys2[ARRAY_SIZE - 1] <= -0.95)
						outerCount2 = 0;
				}

				// Inner Display
				if (innerCount == 0) {
					inner_xs[ARRAY_SIZE - 1] += 0.1;
					if (inner_xs[ARRAY_SIZE - 1] >= 0.55)
						innerCount = 1;
				}
				else if (innerCount == 1) {
					inner_ys[ARRAY_SIZE - 1] += 0.1;
					if (inner_ys[ARRAY_SIZE - 1] >= 0.55)
						innerCount = 2;
				}
				else if (innerCount == 2) {
					inner_xs[ARRAY_SIZE - 1] -= 0.1;
					if (inner_xs[ARRAY_SIZE - 1] <= -0.55)
						innerCount = 3;
				}
				else {
					inner_ys[ARRAY_SIZE - 1] -= 0.1;
					if (inner_ys[ARRAY_SIZE - 1] <= -0.55)
						innerCount = 0;
				}

				// Inner2 Display
				if (innerCount2 == 0) {
					inner_xs2[ARRAY_SIZE - 1] += 0.1;
					if (inner_xs2[ARRAY_SIZE - 1] >= 0.55)
						innerCount2 = 1;
				}
				else if (innerCount2 == 1) {
					inner_ys2[ARRAY_SIZE - 1] += 0.1;
					if (inner_ys2[ARRAY_SIZE - 1] >= 0.55)
						innerCount2 = 2;
				}
				else if (innerCount2 == 2) {
					inner_xs2[ARRAY_SIZE - 1] -= 0.1;
					if (inner_xs2[ARRAY_SIZE - 1] <= -0.55)
						innerCount2 = 3;
				}
				else {
					inner_ys2[ARRAY_SIZE - 1] -= 0.1;
					if (inner_ys2[ARRAY_SIZE - 1] <= -0.55)
						innerCount2 = 0;
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
