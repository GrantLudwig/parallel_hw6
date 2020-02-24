/*
 * Kevin Lundeen
 * CPSC 5002, Seattle University
 * This is free and unencumbered software released into the public domain.
 */

import javax.swing.*;
import java.awt.*;

/**
 * Colored pixel array for swing
 */
@SuppressWarnings("serial")
public class ColoredGrid extends JPanel {
    Color[][] grid;
    
    public ColoredGrid(Color[][] grid) {
        this.grid = grid;
    }
    
    /**
     * Change the grid that is being displayed.
     * @param grid the new grid to display
     */
    public void setGrid(Color[][] grid) {
        this.grid = grid;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int dim = Math.min(getWidth()/getMaxCol(), getHeight()/grid.length);
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                g.setColor(grid[row][col]);
                g.fillRect(col*dim, row*dim, dim, dim);
            }
        }
    }
    
    private int getMaxCol() {
        int max = 0;
        for (Color[] row: grid)
            if (row.length > max)
                max = row.length;
        return max;
    }
}
