/*
* Copyright (C) 2010, 2012 ECOSUR, Andrew Waterman and Max Pimm
*
* Licensed under the Academic Free License v. 3.0.
* http://www.opensource.org/licenses/afl-3.0.php
*/

/**
* @author awaterma@ecosur.mx
*/

package mx.ecosur.multigame.jms;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import mx.ecosur.multigame.ejb.interfaces.SharedBoardLocal;

import mx.ecosur.multigame.enums.GameEvent;

import mx.ecosur.multigame.enums.MoveStatus;
import mx.ecosur.multigame.enums.SuggestionStatus;
import mx.ecosur.multigame.exception.InvalidMoveException;

import mx.ecosur.multigame.exception.InvalidSuggestionException;
import mx.ecosur.multigame.model.interfaces.*;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;


@MessageDriven(mappedName = "MultiGame")
public class AgentListener implements MessageListener {

    @EJB
    private SharedBoardLocal sharedBoard;

    private static final Logger logger = Logger.getLogger(AgentListener.class.getCanonicalName());

    private static final GameEvent[] suggestionEvents = { GameEvent.SUGGESTION_APPLIED, GameEvent.SUGGESTION_EVALUATED};

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void onMessage(Message message) {
        try {
            String gameEvent = message.getStringProperty("GAME_EVENT");
            GameEvent event = GameEvent.valueOf(gameEvent);
            ObjectMessage msg = (ObjectMessage) message;

            if (event.equals(GameEvent.PLAYER_CHANGE)) {
                Game game = (Game) msg.getObject();
                List<GamePlayer> players = game.listPlayers();
                Agent agent = null;
                for (GamePlayer p : players) {
                    if (p instanceof Agent) {
                        Agent a = (Agent) p;
                        if (a.ready()) {
                            agent = a;
                            break;
                        }
                    }
                }

                if (agent != null) {
                    List<Move> moves = agent.determineMoves(game);
                    for (Move m : moves) {
                        m.setPlayerModel(agent);
                        try {
                            sharedBoard.doMove(game, m);
                        } catch (InvalidMoveException e) {
                            logger.warning ("InvalidMove: " + e.getLocalizedMessage() + ". Out of " + moves.size() +
                                    " proposed.")  ;
                            continue;
                        }
                        if (m.getStatus() != MoveStatus.INVALID)
                            break;
                    }

                    if (moves == Collections.EMPTY_LIST || moves.size() == 0)
                        logger.severe("Agent [" + agent.getName() + "] suggested no moves!")  ;
                }
            }


            for (GameEvent possible : suggestionEvents) {
                if (event.equals(possible)) {
                    Suggestion suggestion = (Suggestion) msg.getObject();
                    SuggestionStatus oldStatus = suggestion.getStatus();
                    int gameId = Integer.parseInt(message.getStringProperty("GAME_ID"));
                    Game game = sharedBoard.getGame(gameId);
                    List<GamePlayer> players = game.listPlayers();
                    for (GamePlayer p : players) {
                        if (p instanceof Agent) {
                            Agent agent = (Agent) p;
                            suggestion = (agent.processSuggestion (game, suggestion));
                            SuggestionStatus newStatus = suggestion.getStatus();
                            if (oldStatus != newStatus && (
                                    newStatus.equals(SuggestionStatus.ACCEPT) || newStatus.equals(SuggestionStatus.REJECT)))
                            {
                                sharedBoard.makeSuggestion (game, suggestion);
                            }
                        }
                    }
                }
            }
            /* Acknowledge the durable message */
            msg.acknowledge();
        } catch (JMSException e) {
            logger.severe("Unable to process game message: " + e.getMessage());
        } catch (InvalidSuggestionException e) {
            logger.severe("InvalidSuggestion: " + e.getMessage());
        }
    }
}
