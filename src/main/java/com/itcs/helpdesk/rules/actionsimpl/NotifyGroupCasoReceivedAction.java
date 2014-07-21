/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.rules.actionsimpl;

import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.Grupo;
import com.itcs.helpdesk.rules.Action;
import com.itcs.helpdesk.rules.ActionExecutionException;
import com.itcs.helpdesk.util.Log;
import com.itcs.helpdesk.util.MailNotifier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;

/**
 *
 * @author jonathan
 */
public class NotifyGroupCasoReceivedAction extends Action {

//    public NotifyGroupCasoReceivedAction(JPAServiceFacade jpaController, ManagerCasos managerCasos) {
//        super(jpaController, managerCasos);
//    }

    @Override
    public void execute(Caso caso, String schema) throws ActionExecutionException {
        final EntityManager em = createEntityManager(schema);

        caso = em.find(Caso.class, caso.getIdCaso());
        Log.createLogger(NotifyGroupCasoReceivedAction.class.getName()).logSevere("NotifyGroupCasoReceivedAction.execute on " + caso);
        if (caso.getIdProducto() != null) {
            for (Grupo grupo : caso.getIdProducto().getGrupoList()) {
                try {
                    MailNotifier.notifyGroupCasoReceived(grupo, caso, schema);
                } catch (Exception ex) {
                    Logger.getLogger(MailNotifier.class.getName()).log(Level.SEVERE, "Error al tratar de enviar caso por email al grupo " + grupo + " favor verifique la configuración de correo.", ex);
                }
            }
        }

    }
}
