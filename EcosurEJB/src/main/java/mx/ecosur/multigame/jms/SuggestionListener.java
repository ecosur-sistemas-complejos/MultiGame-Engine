/*
* Copyright (C) 2008-2012 ECOSUR, Andrew Waterman and Max Pimm
*
* Licensed under the Academic Free License v. 3.0.
* http://www.opensource.org/licenses/afl-3.0.php
*/

package mx.ecosur.multigame.jms;

import mx.ecosur.multigame.ejb.interfaces.SharedBoardLocal;
import mx.ecosur.multigame.enums.GameState;
import mx.ecosur.multigame.enums.SuggestionStatus;
import mx.ecosur.multigame.exception.InvalidSuggestionException;
import mx.ecosur.multigame.model.interfaces.Agent;
import mx.ecosur.multigame.model.interfaces.Game;
import mx.ecosur.multigame.model.interfaces.GamePlayer;
import mx.ecosur.multigame.model.interfaces.Suggestion;

import javax.ejb.*;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.List;
import java.util.logging.Logger;

/**
 * An MDB for handling Suggestions.
 *
 * @author Andrew Waterman <awaterma@ecosur.mx>
 */
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@MessageDriven(mappedName = "MultiGame", activationConfig = {
        @ActivationConfigProperty(
                propertyName = "messageSelector", propertyValue = "GAME_EVENT = 'SUGGESTION_EVALUATED'"),
        @ActivationConfigProperty(
                propertyName = "destinationType", propertyValue = "javax.jms.Topic")})
public class SuggestionListener implements MessageListener {

    @EJB
    private SharedBoardLocal sharedBoard;

    private static final Logger logger = Logger.getLogger(SuggestionListener.class.getCanonicalName());

    public void onMessage(Message message) {
        try {
            ObjectMessage msg = (ObjectMessage) message;
            Object [] data = (Object[]) msg.getObject();
            Game game = (Game) data[ 0 ];
            if (game.getState().equals(GameState.PLAY)) {
                Suggestion suggestion = (Suggestion) data[ 1 ];
                List<GamePlayer> players = game.listPlayers();
                for (GamePlayer p : players) {
                    if (p instanceof Agent) {
                        Agent agent = (Agent) p;
                        suggestion = (agent.processSuggestion(game, suggestion));
                        SuggestionStatus status = suggestion.getStatus();
                        if (status == SuggestionStatus.ACCEPT || status == SuggestionStatus.REJECT) {
                            sharedBoard.makeSuggestion(game, suggestion);
                            break;
                        }
                    }
                }
            }
        } catch (JMSException e) {
            logger.severe("Unable to process game message: " + e.getMessage());
        } catch (InvalidSuggestionException e) {
            logger.severe("Unable to process game message: " + e.getMessage());
        }
    }
}
