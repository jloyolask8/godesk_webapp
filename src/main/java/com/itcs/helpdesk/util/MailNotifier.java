package com.itcs.helpdesk.util;

import com.itcs.commons.email.EmailClient;
import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.Grupo;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.persistence.entityenums.EnumAreas;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoCaso;
import com.itcs.helpdesk.quartz.HelpDeskScheluder;
import com.itcs.helpdesk.util.MailClientFactory.MailNotConfiguredException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.mail.EmailException;

/**
 *
 * @author Jonathan
 */
public class MailNotifier {

    /**
     * TODO use no-reply@godesk.cl
     * @param current
     * @param motivo
     * @param schema
     * @throws com.itcs.helpdesk.util.MailClientFactory.MailNotConfiguredException
     * @throws EmailException 
     */
    public static void notifyCasoAssigned(Caso current, String motivo, String schema) throws MailNotConfiguredException, EmailException {
        if (current != null) {
            String asunto = ApplicationConfig.getInstance(schema).getNotificationSubjectText(); //may contain place holders
            String newAsunto = ManagerCasos.formatIdCaso(current.getIdCaso()) + " " + ClippingsPlaceHolders.buildFinalText(asunto, current, schema);

            String texto = ApplicationConfig.getInstance(schema).getNotificationBodyText();//may contain place holders 
            texto = texto + "<b>Motivo:<b/> " + (motivo != null ? motivo : "Pronta atención del caso");
            String newTexto = ClippingsPlaceHolders.buildFinalText(texto, current, schema);

            if (current.getIdArea().getEmailEnabled() != null && current.getIdArea().getEmailEnabled()) {

                if (current.getIdArea() != null) {
                    MailClientFactory.getInstance(schema, current.getIdArea().getIdArea())
                            .sendHTML(current.getOwner().getEmail(), newAsunto,
                                    newTexto, null);
                } else {
                    MailClientFactory.getInstance(schema, EnumAreas.DEFAULT_AREA.getArea().getIdArea())
                            .sendHTML(current.getOwner().getEmail(), newAsunto,
                                    newTexto, null);
                }

            } else {
                throw new EmailException("No se puede enviar el correo. El Area Asociada al caso tiene el envío de correos desabilitado.");
            }

        }

    }

    /**
     * TODO use no-reply@godesk.cl
     * @param current
     * @param who
     * @param schema
     * @throws com.itcs.helpdesk.util.MailClientFactory.MailNotConfiguredException
     * @throws EmailException 
     */
    public static void notifyCasoAsHtmlEmail(Caso current, String[] who, String schema) throws MailNotConfiguredException, EmailException {
        if (current != null) {
            String asunto = "Notificacion de caso: " + current.getTipoCaso().getNombre() + " #" + current.getIdCaso();//ApplicationConfig.getNotificationSubjectText(); //may contain place holders
//            String newAsunto = ClippingsPlaceHolders.buildFinalText(asunto, current);

            String texto = CasoExporter.exportToHtmlText(current);//ApplicationConfig.getNotificationBodyText();//may contain place holders 
//            String newTexto = ClippingsPlaceHolders.buildFinalText(texto, current);

            if (current.getIdArea().getEmailEnabled() != null && current.getIdArea().getEmailEnabled()) {

                if (current.getIdArea() != null) {
                    MailClientFactory.getInstance(schema, current.getIdArea().getIdArea())
                            .sendHTML(who, asunto,
                                    texto, null);
                } else {
                    MailClientFactory.getInstance(schema, EnumAreas.DEFAULT_AREA.getArea().getIdArea())
                            .sendHTML(who, asunto,
                                    texto, null);
                }

            } else {
                throw new EmailException("No se puede enviar el correo. El Area Asociada al caso tiene el envío de correos desabilitado.");
            }

        }

    }

    /**
     * This email notification must be considered as a note that customer can reply.
     * @param current
     * @param schema
     * @return
     * @throws com.itcs.helpdesk.util.MailClientFactory.MailNotConfiguredException
     * @throws EmailException 
     */
    public static String emailClientCasoUpdatedByAgent(Caso current, String schema) throws MailNotConfiguredException, EmailException {
        //TODO: use configure texts.
        if (current != null && current.getEmailCliente() != null) {
            String asunto = ApplicationConfig.getInstance(schema).getNotificationClientSubjectText(); //may contain place holders
            String newAsunto = ManagerCasos.formatIdCaso(current.getIdCaso()) + " " + ClippingsPlaceHolders.buildFinalText(asunto, current, schema);

            String texto = ApplicationConfig.getInstance(schema).getNotificationClientBodyText();//may contain place holders 

            String newTexto = ClippingsPlaceHolders.buildFinalText(texto, current, schema);

            EmailClient ec = MailClientFactory.getInstance(schema, current.getIdArea().getIdArea());
            if (ec != null) {
                ec.sendHTML(current.getEmailCliente().getEmailCliente(), newAsunto,
                        newTexto, null);
                return newTexto;
            } else {
                return null;
            }

        }

        return null;

    }

    /**
     * TODO: Remove getTextoRespAutomatica & getSubjectRespAutomatica from AREA.
     * Use Same for all (config)
     *
     * @param caso
     */
    public static void emailClientCasoReceived(Caso caso, String schema) {
        //TODO: use configure texts.
        if (caso != null) {
            try {
                String mensaje_ = "";
                String subject_ = "";
                if (caso.getIdArea() != null) {
                    if (caso.getIdArea().getTextoRespAutomatica() != null) {
                        mensaje_ = (ClippingsPlaceHolders.buildFinalText(caso.getIdArea().getTextoRespAutomatica(), caso, schema));
                    }

                    if (caso.getIdArea().getSubjectRespAutomatica() != null) {
                        subject_ = (ClippingsPlaceHolders.buildFinalText(caso.getIdArea().getSubjectRespAutomatica(), caso, schema));
                    }
                }

                final String subject = ManagerCasos.formatIdCaso(caso.getIdCaso()) + " " + subject_;
                final String mensaje = mensaje_;

                MailClientFactory.getInstance(schema, caso.getIdArea().getIdArea())
                        .sendHTML(caso.getEmailCliente().getEmailCliente(), subject,
                                mensaje, null);
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.getLogger(MailNotifier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    
    
    /**
     * TODO use no-reply@godesk.cl
     * @param grupo
     * @param caso
     * @param schema 
     */
    public static void notifyGroupCasoReceived(final Grupo grupo, Caso caso, String schema) {
        Logger.getLogger(MailNotifier.class.getName()).log(Level.INFO, "notifyAgentsCasoReceived:{0}", grupo);
        //TODO: use configure texts.
        if (caso != null) {
            try {
                final String subject = "GoDesk " + (caso.getIdCanal() != null ? caso.getIdCanal().getNombre() : "")
                        + " " + (caso.getTipoCaso() != null ? caso.getTipoCaso().getNombre() : EnumTipoCaso.CONTACTO.getTipoCaso().getNombre())
                        + " #" + caso.getIdCaso();//ApplicationConfig.getNotificationSubjectText(); //may contain place holders

                if (grupo.getUsuarioList() != null && !grupo.getUsuarioList().isEmpty()) {

                    StringBuilder to = new StringBuilder();
                    boolean first = true;
                    for (Usuario usuario : grupo.getUsuarioList()) {
                        if (first) {
                            to.append(usuario.getEmail());
                            first = false;
                        } else {
                            to.append(",").append(usuario.getEmail());
                        }
                    }
                    String mensaje = "Estimado Agente,<br/><br/>"
                            + "Le notificamos que existe un caso en su grupo (" + grupo.getNombre() + ") para su pronta atención. "
                            + "Favor contactar al cliente lo antes posible.<br/>";

                    final String mensajeFinal = mensaje + CasoExporter.exportToHtmlText(caso);
                    final String idArea = grupo.getIdArea().getIdArea();

                    try {
                        HelpDeskScheluder.scheduleSendMailNow(schema, idArea, mensajeFinal, to.toString(), subject);
                    } catch (Exception ex) {
                        //It may fail right away, we still continue
                        Logger.getLogger(MailNotifier.class.getName()).log(Level.SEVERE, "scheduleNotifyAgentsCasoReceived failed", ex);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(MailNotifier.class.getName()).log(Level.SEVERE, "notifyAgentsCasoReceived", ex);
            }
        }
    }

}
