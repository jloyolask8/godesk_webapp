/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.rules.actionsimpl;

import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.rules.Action;
import com.itcs.helpdesk.rules.ActionExecutionException;
import com.itcs.helpdesk.util.Log;
import com.itcs.helpdesk.util.MailNotifier;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jonathan
 */
public class SendCaseByEmailAction extends Action implements Serializable {

//    public static final String EMAIL_TO_FIELD_KEY = "emailsToFieldKey";
//    public static final String ACTION_CLASS = "actionclass";
//    public SendCaseByEmailAction(JPAServiceFacade jpaController, ManagerCasos managerCasos) {
//        super(jpaController, managerCasos);
//        //Persistence.createEntityManagerFactory(null, null)
//         // PersistenceUnitProperties.ALLOW_NATIVE_SQL_QUERIES      
////        PersistenceUnitProperties.M
//    }
    @Override
    public void execute(final Caso caso, final String schema) throws ActionExecutionException {

        Log.createLogger(SendCaseByEmailAction.class.getName()).logSevere("SendCaseByEmailAction.execute on " + caso);

        try {
            String destinationEmails = getConfig();//Gets the parametros, setted 
            if (StringUtils.isEmpty(destinationEmails)) {
                throw new ActionExecutionException("Esta accion necesita parametros (destination Email addresses) para poder executarse.");
            } else {
                String[] emails = destinationEmails.split(",");
                MailNotifier.notifyCasoAsHtmlEmail(caso, emails, schema);
                
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ActionExecutionException("Error al tratar de enviar caso por email, "
                    + "favor verifique la configuración de correo o los destinatarios.", ex);
        }

    }
}
