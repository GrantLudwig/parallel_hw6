/*
 * Grant Ludwig
 * CPSC 4600, Seattle University
 * HeatMapDriver.java
 * Adapted from HeatMap5.java created by Kevin Lundeen
 * 2/28/20
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

public class HeatMapDriver {
	private static final int DIM = 20;
	private static final int SLEEP_INTERVAL = 50; // milliseconds
	//private static final String FILENAME = "observation_gaussian.dat";
	//private static final String FILENAME = "observation_scan.dat";
	//private static final String FILENAME = "observation_easy.dat";
	//private static final String FILENAME = "observation_gaussianSmallLarge.dat";
	//private static final String FILENAME = "observation_grant.dat";
	private static final String FILENAME = "observation_snake.dat";
	private static final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;
	private static final double HOT_CALIB = 1.0;
	private static final String REPLAY = "Replay";
	private static final int N_THREADS = 16;
	private static final int SAMPLING_TIME = 1; // ms
	private static JFrame application;
	private static JButton button;
	private static Color[][] grid;
	private static int current;
	private static List<HeatMap> finalHeatmaps;

	// Heatmaps are indexed by time
	// Obervations are the observations in time

	/**
	 * List of HeatMaps returned is indexed by their time
	 */
	static class HeatScan extends GeneralScan3<ArrayList<Observation>, List<HeatMap>> {
		private int numTimes;

		/**
		 * Constructor
		 * @param raw ArrayList of ArrayLists of Observations
		 *            Indexed by the time stamp they occur at
		 */
		public HeatScan(ArrayList<ArrayList<Observation>> raw) {
			super(raw, 100);
			this.numTimes = raw.size();
		}

		@Override
		protected List<HeatMap> init() {
			List<HeatMap> heatMaps = new ArrayList<HeatMap>(numTimes);
			for (int i = 0; i < numTimes; i++)
				heatMaps.add(i, new HeatMap(DIM, -1.0, +1.0));
			return heatMaps;
		}

		@Override
		protected List<HeatMap> prepare(ArrayList<Observation> datum) {
			List<HeatMap> heatMaps = init();
			for (int i = 0; i < datum.size(); i++) {
				heatMaps.set((int)datum.get(i).time, new HeatMap(datum.get(i).x, datum.get(i).y));
			}
			return heatMaps;
		}

		@Override
		protected List<HeatMap> combine(List<HeatMap> left, List<HeatMap> right) {
			List<HeatMap> heatMaps = init();
			for (int i = 0; i < left.size(); i++) {
				heatMaps.set(i, HeatMap.combine(left.get(i), right.get(i)));
			}
			return heatMaps;
		}

		@Override
		protected void accum(List<HeatMap> hm, ArrayList<Observation> datum) {
			for (int i = 0; i < datum.size(); i++) {
				hm.get((int)datum.get(i).time).accum(datum.get(i).x, datum.get(i).y);
			}
		}
	}

	/**
	 * @param args Not used
	 * @throws FileNotFoundException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		// Each ArrayList of Observations is full of observations that occured at that time
		// The ArrayList of ArrayLists holds all the different times. Indexed by time stamp
		// There can be empty ArrayLists of observations
		ArrayList<ArrayList<Observation>> observations = new ArrayList<ArrayList<Observation>>();
		System.out.println("Reading from " + FILENAME);
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
			int timeNum = 0;
			Observation obs = (Observation) in.readObject();
			observations.add(new ArrayList<Observation>());
			while (!obs.isEOF()) {
				// add empty observations till we reach time step we need
				while (timeNum < obs.time) {
					observations.add(new ArrayList<Observation>());
					timeNum++;
				}
				observations.get(timeNum).add(obs);
				obs = (Observation) in.readObject();
			}
			in.close();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("reading from " + FILENAME + "failed: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		// Create a HeatScan and reduce it
		// Reduction is indexed by every time step. Adds all the events that occured at that time
		HeatScan thing = new HeatScan(observations);
		List<HeatMap> heatmaps = thing.getReduction();
		System.out.println("Reduction Complete");

		// Complete the second pass
		finalHeatmaps = decayedHeatmaps(heatmaps);
		System.out.println("Second Pass Complete");
		System.out.println("Beginning Animation sampling every " + SAMPLING_TIME + " ms");

		current = 0;

		grid = new Color[DIM][DIM];
		application = new JFrame();
		application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fillGrid(grid);

		ColoredGrid gridPanel = new ColoredGrid(grid);
		application.add(gridPanel, BorderLayout.CENTER);

		button = new JButton(REPLAY);
		button.addActionListener(new BHandler());
		application.add(button, BorderLayout.PAGE_END);

		application.setSize(DIM * 40, (int) (DIM * 40.4));
		application.setVisible(true);
		application.repaint();
		animate();
	}

	/**
	 * Runs and setups the second pass to decay heatmaps
	 * @param heatMaps
	 * @return List of Heatmaps, that are properly decayed and at the sampling time desired
	 */
	private static List<HeatMap> decayedHeatmaps(List<HeatMap> heatMaps) {
		int N = heatMaps.size();
		double numElements = (int) Math.ceil((double) N / (double) SAMPLING_TIME); // get the numElements to sample
		int elementsPerThread = (int) Math.ceil(numElements / (double) N_THREADS); // gets the numElements to sample per thread
		int startIndex = 0;
		Thread[] mapThreads = new Thread[N_THREADS];
		ArrayList<ArrayList<HeatMap>> writeHeatmaps = new ArrayList<ArrayList<HeatMap>>(); // Create an ArrayList for each thread to write to

		// Setup and start threads
		for (int i = 0; i < N_THREADS; i++) {
			int 	endIndex,
					calcIndex = startIndex + (SAMPLING_TIME * elementsPerThread) - 1;

			// setup endIndex
			if (calcIndex >= N)
				endIndex = N - 1;
			else
				endIndex = calcIndex;

			// create ArrayList of HeatMap for thread to write to
			writeHeatmaps.add(new ArrayList<HeatMap>());

			// start threads
			mapThreads[i] = new Thread(new SecondPass(heatMaps, writeHeatmaps.get(i), startIndex, endIndex, SAMPLING_TIME));
			mapThreads[i].start();
			startIndex = endIndex + 1;

			// If have more threads than needed
			if (startIndex >= N)
				break;
		}

		List<HeatMap> returnMaps = new ArrayList<HeatMap>();

		// join threads and combine results
		for (int i = 0; i < writeHeatmaps.size(); i++) {
			try {
				mapThreads[i].join();
				for (int j = 0; j < writeHeatmaps.get(i).size(); j++) {
					returnMaps.add(writeHeatmaps.get(i).get(j));
				}
			}
			catch(InterruptedException e) {
				System.out.println("Error joining threads.\n" + e);
			}
		}

		return returnMaps;
	}

	/**
	 * Animates the HeatMaps
	 * @throws InterruptedException
	 */
	private static void animate() throws InterruptedException {
		button.setEnabled(false);
		for (current = 0; current < finalHeatmaps.size(); current++) {
			fillGrid(grid);
			application.repaint();
			Thread.sleep(SLEEP_INTERVAL);
		}
		button.setEnabled(true);
		application.repaint();
	}

	/**
	 * Handles button presses
	 */
	static class BHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (REPLAY.equals(e.getActionCommand())) {
				new Thread() {
					public void run() {
						try {
							animate();
						} catch (InterruptedException e) {
							System.exit(0);
						}
					}
				}.start();
			}
		}
	};

	/**
	 * Fills the screen with our heatmap
	 * @param grid
	 */
	private static void fillGrid(Color[][] grid) {
		for (int r = 0; r < grid.length; r++)
			for (int c = 0; c < grid[r].length; c++) {
				grid[r][c] = interpolateColor(finalHeatmaps.get(current).getCell(r, c) / HOT_CALIB, COLD, HOT);
			}
	}

	/**
	 * Figures out the color to display of out heatmap
	 * @param ratio
	 * @param a
	 * @param b
	 * @return
	 */
	private static Color interpolateColor(double ratio, Color a, Color b) {
		//System.out.println("Ratio: " + ratio);
		ratio = Math.min(ratio, 1.0);
		int ax = a.getRed();
		int ay = a.getGreen();
		int az = a.getBlue();
		int cx = ax + (int) ((b.getRed() - ax) * ratio);
		int cy = ay + (int) ((b.getGreen() - ay) * ratio);
		int cz = az + (int) ((b.getBlue() - az) * ratio);
		return new Color(cx, cy, cz);
	}

	/**
	 * Class for running the second pass
	 * Implements Runnable
	 */
	public static class SecondPass implements Runnable {
		private int start, // start index in readHeatMaps for thread to read
					end; // end index in readHeatMaps for thread to read
		private List<HeatMap> readHeatMaps; // List the thread will read from
		private List<HeatMap> writeHeatMaps; // List the thread will write to
		private int sampleRate; // Sampling rate, will sample times at this rate

		/**
		 * Constructor
		 * @param rhm List of HeatMaps to read from, indexed by occured time
		 * @param whm List of HeatMaps to write to
		 * @param start Start index for the thread to read from rhm
		 * @param end End index for the thread to read from rhm
		 * @param sampleRate Time rate to sample the data at
		 */
		public SecondPass(List<HeatMap> rhm, List<HeatMap> whm, int start, int end, int sampleRate) {
			this.start = start;
			this.end = end;
			this.readHeatMaps = rhm;
			this.writeHeatMaps = whm;
			this.sampleRate = sampleRate;
		}

		/**
		 * Runs the second pass
		 */
		@Override
		public void run() {
			for (int i = start; i <= end; i += sampleRate) {
				int minIndex = i - 100;
				if (minIndex < 0)
					minIndex = 0;
				HeatMap tempMap = readHeatMaps.get(i);
				tempMap.normalize(); // normalizes the data
				// runs through the previous 100 times and adds to the final time heatmap, will be properly decayed
				for (int j = minIndex; j < i; j++)
					tempMap.addWeighted(readHeatMaps.get(j), weight(i, j));
				writeHeatMaps.add(tempMap);
			}
		}

		/**
		 * Calulates the proper weight for the observation
		 * @param time Current time step
		 * @param timeOfObservation Time step of the observation
		 * @return double of the weight
		 */
		private static double weight(int time, int timeOfObservation) {
			double temp = 1.0 / (double) (1.0 + time - timeOfObservation);
			return temp;
		}
	}
}
