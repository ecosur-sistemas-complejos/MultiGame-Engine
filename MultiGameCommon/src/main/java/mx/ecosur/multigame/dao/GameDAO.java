package mx.ecosur.multigame.dao;

import mx.ecosur.multigame.model.interfaces.Game;
import mx.ecosur.multigame.model.interfaces.GamePlayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * GameDAO is a simple Data Access Object for representing GridGames to downstream consumers (e.g. Flex, BlazeDS,
 * JavaScript, etc.) for display.
 *
 * @author awaterma@ecosur.mx
 */
public class GameDAO implements Serializable {
    
    private int gameId;
    
    private Date creationDate;
    
    private String gameType;
    
    private List<String> players;
    
    private String status;
    
    public GameDAO () {
        super();
    }
    
    public GameDAO (Game game) {
        this();
        this.gameId = game.getId();
        this.creationDate = game.getCreated();
        this.gameType = game.getGameType();
        this.status = game.getState().name();
        this.players = new ArrayList<String>();
        for (GamePlayer p : game.listPlayers()) {
            this.players.add(p.getName());
        }
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
