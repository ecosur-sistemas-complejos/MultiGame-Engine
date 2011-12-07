package mx.ecosur.multigame.grid.comparator;

import mx.ecosur.multigame.grid.Color;
import mx.ecosur.multigame.grid.entity.GridPlayer;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: awaterma
 * Date: 3/15/11
 * Time: 1:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayerComparator implements Comparator<GridPlayer>, Serializable {

    @Override
    public int compare(GridPlayer a, GridPlayer b) {
        // Color order is based on natural numbering of Color enum
        return (a.getColor().compareTo(b.getColor()));
    }
}
