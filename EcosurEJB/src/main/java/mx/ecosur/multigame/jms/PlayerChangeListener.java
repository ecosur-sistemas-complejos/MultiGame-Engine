/*
* Copyright (C) 2008-2012 ECOSUR, Andrew Waterman and Max Pimm
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

import mx.ecosur.multigame.enums.GameState;
import mx.ecosur.multigame.enums.MoveStatus;
import mx.ecosur.multigame.exception.InvalidMoveException;
import mx.ecosur.multigame.model.interfaces.*;

import javax.ejb.*;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@MessageDriven(mappedName = "MultiGame",  activationConfig = {
        @ActivationConfigProperty(
            propertyName = "messageSelector", propertyValue = "GAME_EVENT = 'PLAYER_CHANGE'"),
        @ActivationConfigProperty(
            propertyName = "destinationType", propertyValue = "javax.jms.Topic")})
public class PlayerChangeListener implements MessageListener {

    @EJB
    private SharedBoardLocal sharedBoard;

    private static final Logger logger = Logger.getLogger(PlayerChangeListener.class.getCanonicalName());

    public void onMessage(Message message) {
        try {
            ObjectMessage msg = (ObjectMessage) message;
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
                        continue;
                    }
                    if (m.getStatus() != MoveStatus.INVALID) {
                        break;
                    }
                }

                if (moves == Collections.EMPTY_LIST || moves.size() == 0) {
                    logger.severe("Agent [" + agent.getName() + "] suggested no moves!");
                }
            }

            /* Acknowledge the durable message */
            msg.acknowledge();

        } catch (JMSException e) {
            logger.severe("Unable to process game message: " + e.getMessage());
        }
    }
}
