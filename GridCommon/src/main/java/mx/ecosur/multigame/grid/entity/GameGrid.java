/*
 * Copyright (C) 2010 ECOSUR, Andrew Waterman and Max Pimm
 *
 * Licensed under the Academic Free License v. 3.0.
 * http://www.opensource.org/licenses/afl-3.0.php
 */

/**
 * The GameGrid class holds the current state of a specific game.  This class
 * is intended to be a transitive object, in the sense that a specific 
 * SharedBoardwill return a populated GameGrid to a specific caller. This 
 * hides gamegrid specific information for runtime callers; allowing quick
 * access to specific cells within a running game.
 * 
 * @author awaterma@ecosur.mx
 *
 */

package mx.ecosur.multigame.grid.entity;

import java.io.Serializable;
import java.util.*;

import javax.persistence.*;

import mx.ecosur.multigame.grid.comparator.CellComparator;

@Entity
public class GameGrid implements Serializable, Cloneable {

    private static final long serialVersionUID = -2579204312184918693L;

    Set<GridCell> cells;

    private int id;

    public GameGrid () {
        super();
    }

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GridCell getLocation (GridCell location) {
        /* Simple consecutive search o(N) time */
        if (cells != null) {
            for (GridCell g : cells) {
                if (g.equals(location)) {
                    return g;
                }
            }
        }

        return null;
    }

    public void updateCell (GridCell cell) {
        if (cells == null)
            cells = new HashSet<GridCell>();
        if (cells.contains(cell))
                cells.remove(cell);
        cells.add(cell);
    }

    public void removeCell (GridCell cell) {
        if (cells != null)
            cells.remove(cell);
    }


    public boolean isEmpty() {
        return (cells == null || cells.size() == 0);
    }

    public void setEmpty (boolean disregard) {
        /* Does nothing */
    }

    @OneToMany (cascade={CascadeType.ALL}, fetch=FetchType.EAGER)
    @OrderBy("row,column")
    public Set<GridCell> getCells () {
        return cells;
    }

    public void setCells(Set<GridCell> c){
        cells = c;
    }

    public String toString () {
        StringBuffer buf = new StringBuffer();
        buf.append ("GameGrid: [");
        if (cells != null && cells.size() > 0) {
            Iterator<GridCell> iter = cells.iterator();
            while (iter.hasNext()) {
                GridCell cell = iter.next();
                buf.append("column: " + cell.getColumn() + ", row: " +
                            cell.getRow() + ", color: " + cell.getColor());
                if (iter.hasNext()) {
                    buf.append ("; ");
                }
                else {
                    buf.append ("]");
                }
            }
        }

        return buf.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        GameGrid ret = new GameGrid();
        if (!isEmpty()) {
            Set<GridCell> cloneCells = new HashSet<GridCell>();
            ret.setCells(cloneCells);
            for (GridCell cell : cells) {
                GridCell cloneCell = cell.clone();
                ret.getCells().add(cloneCell);
            }
        }

        return ret;
    }
}
