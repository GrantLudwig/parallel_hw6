/*
 * Kevin Lundeen
 * Fall 2018, CPSC 5600, Seattle University
 * This is free and unencumbered software released into the public domain.
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

/**
 * Step 5 from HW5
 */
public class HeatMap5 {
	private static final int DIM = 20;
	private static final int SLEEP_INTERVAL = 50; // milliseconds
	private static final String FILENAME = "observation_gaussian.dat";
	private static final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;
	private static final double HOT_CALIB = 1.0;
	private static final String REPLAY = "Replay";
	private static JFrame application;
	private static JButton button;
	private static Color[][] grid;
	private static int current;
	private static List<HeatMap> heatmaps;

	static class HeatScan extends GeneralScan3<Observation, HeatMap> {

		public HeatScan(List<Observation> raw) {
			super(raw, 100);
		}

		@Override
		protected HeatMap init() {
			return new HeatMap();
		}

		@Override
		protected HeatMap prepare(Observation datum) {
			return new HeatMap(datum.x, datum.y);
		}

		@Override
		protected HeatMap combine(HeatMap left, HeatMap right) {
			return HeatMap.combine(left, right);
		}

		@Override
		protected void accum(HeatMap hm, Observation datum) {
			hm.accum(datum.x, datum.y);
		}
	}

	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		List<Observation> data = new ArrayList<Observation>();
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
			// int count = 0;
			Observation obs = (Observation) in.readObject();
			while (!obs.isEOF()) {
				// System.out.println(++count + ": " + obs);
				data.add(obs);
				obs = (Observation) in.readObject();
			}
			in.close();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("reading from " + FILENAME + "failed: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		HeatScan scanner = new HeatScan(data);
		// System.out.println("reduction: " + scanner.getReduction());
		heatmaps = scanner.getScan();
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

	private static void animate() throws InterruptedException {
		button.setEnabled(false);
		for (current = 0; current < heatmaps.size(); current++) {
			fillGrid(grid);
			application.repaint();
			Thread.sleep(SLEEP_INTERVAL);
		}
		button.setEnabled(true);
		application.repaint();
	}

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

	private static void fillGrid(Color[][] grid) {
		for (int r = 0; r < grid.length; r++)
			for (int c = 0; c < grid[r].length; c++)
				grid[r][c] = interpolateColor(heatmaps.get(current).getCell(r, c) / HOT_CALIB, COLD, HOT);
	}

	private static Color interpolateColor(double ratio, Color a, Color b) {
		ratio = Math.min(ratio, 1.0);
		int ax = a.getRed();
		int ay = a.getGreen();
		int az = a.getBlue();
		int cx = ax + (int) ((b.getRed() - ax) * ratio);
		int cy = ay + (int) ((b.getGreen() - ay) * ratio);
		int cz = az + (int) ((b.getBlue() - az) * ratio);
		return new Color(cx, cy, cz);
	}

}
