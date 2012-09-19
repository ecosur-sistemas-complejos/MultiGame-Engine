/*
* Copyright (C) 2010 ECOSUR, Andrew Waterman and Max Pimm
*
* Licensed under the Academic Free License v. 3.0.
* http://www.opensource.org/licenses/afl-3.0.php
*/


/**
 * The SharedBoardEJB handles operations between players and the shared game
 * board.  The SharedBoardEJB manges game specific events, such as validating a
 * specific move on a game board, making a specific move, and modifying a 
 * previous move.  Clients can also add chat messages to the message stream,
 * increment players turns (soon to be phased into the game rules), and get
 * a list of players for a specific game.
 * 
 * @author awaterma@ecosur.mx
 */

package mx.ecosur.multigame.ejb.impl;

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.*;

import mx.ecosur.multigame.ejb.interfaces.SharedBoardLocal;
import mx.ecosur.multigame.ejb.interfaces.SharedBoardRemote;
import mx.ecosur.multigame.enums.GameState;
import mx.ecosur.multigame.enums.SuggestionStatus;
import mx.ecosur.multigame.exception.InvalidMoveException;

import mx.ecosur.multigame.enums.MoveStatus;

import mx.ecosur.multigame.exception.InvalidSuggestionException;
import mx.ecosur.multigame.grid.entity.GridGame;
import mx.ecosur.multigame.model.interfaces.*;

@Stateless
@RolesAllowed("MultiGame")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SharedBoard implements SharedBoardLocal, SharedBoardRemote {


    private static final Logger logger;

    static {
        logger = Logger.getLogger(SharedBoard.class.getCanonicalName());
    }

    @PersistenceContext (unitName = "MultiGamePU")
    EntityManager em;

    public SharedBoard () throws InstantiationException, IllegalAccessException,
            ClassNotFoundException
    {
        super();
    }

    /* (non-Javadoc)
     * @see mx.ecosur.multigame.ejb.SharedBoardLocal#getGame(int)
     */
    public Game getGame(int gameId) {
        return em.find(GridGame.class, gameId);
    }

    /* (non-Javadoc)
     * @see mx.ecosur.multigame.ejb.SharedBoardRemote#move(mx.ecosur.multigame.entity.Move)
     */
    public Move doMove(Game game, Move move) throws InvalidMoveException {
        em.joinTransaction();
        move = em.merge(move);
        game = em.find(game.getClass(), game.getId());
        game.move (move);
        em.flush();

        if (move.getStatus().equals(MoveStatus.INVALID))
            throw new InvalidMoveException ("INVALID Move. [" + move.toString() + "]");
        else if (move.getStatus().equals(MoveStatus.EXPIRED))
            throw new InvalidMoveException("EXPIRED move. [ " + move.toString() + "]");

        /* Good move? Game still in progress? Message downstream players */
        if (move.getStatus() != MoveStatus.INVALID && game.getState() == GameState.PLAY) {
            game.getMessageSender().sendPlayerChange(game);
        }
        return move;
    }


    public Suggestion makeSuggestion(Game game, Suggestion suggestion) throws InvalidSuggestionException {
        em.joinTransaction();
        suggestion = em.merge(suggestion);
        game = em.find(game.getClass(), game.getId());
        suggestion = game.suggest(suggestion);
        em.flush();

        if (suggestion.getStatus().equals(SuggestionStatus.INVALID))
            throw new InvalidSuggestionException ("INVALID Move suggested!");
        return suggestion;
    }

    /* (non-Javadoc)
     * @see mx.ecosur.multigame.ejb.SharedBoardRemote#getMoves(int)
     */
    public Collection<Move> getMoves(int gameId) {
        Game game = em.find(GridGame.class, gameId);
        return game.listMoves();
    }

    public ChatMessage addMessage(ChatMessage chatMessage) {
        em.joinTransaction();
        return em.merge (chatMessage);
    }

    /* (non-Javadoc)
     * @see mx.ecosur.multigame.ejb.interfaces.SharedBoardInterface#updateMove(mx.ecosur.multigame.entity.Move)
     */
    public Move updateMove(Move move) {
        em.joinTransaction();
        /* Get current state of player */
        GamePlayer p = move.getPlayerModel();
        p = (GamePlayer) em.find(p.getClass(), p.getId());
        move.setPlayerModel(p);
        return em.merge(move);
    }
}
