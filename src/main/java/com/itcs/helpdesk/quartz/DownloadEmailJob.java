/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.quartz;

import com.itcs.commons.email.EmailClient;
import com.itcs.commons.email.EmailMessage;
import com.itcs.helpdesk.persistence.entities.Area;
import com.itcs.helpdesk.persistence.entities.BlackListEmail;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import com.itcs.helpdesk.util.ApplicationConfig;
import com.itcs.helpdesk.util.Log;
import com.itcs.helpdesk.util.MailClientFactory;
import com.itcs.helpdesk.util.ManagerCasos;
import com.itcs.helpdesk.util.RulesEngine;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.ee.jta.UserTransactionHelper;

/**
 *
 * @author jonathan
 */
public class DownloadEmailJob extends AbstractGoDeskJob implements Job {

    
    /**
     * {0} = schema {1} = idArea#
     */
    public static final String JOB_ID = "%s_DownloadEmailArea_%s";

    public static String formatJobId(String schema, String idArea) {
        return String.format(JOB_ID, new Object[]{schema, idArea});
    }

//    public DownloadEmailJob(JPAServiceFacade jpaController, ManagerCasos managerCasos) {
//        super(jpaController, managerCasos);
//    }
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap map = context.getMergedJobDataMap();//.getJobDetail().getJobDataMap();
        if (map != null) {
            String idArea = (String) map.get(ID_AREA);
            String interval = (String) map.get(INTERVAL_SECONDS);
            String schema = (String) map.get(ID_TENANT);
            try {
                if (!StringUtils.isEmpty(idArea) && !StringUtils.isEmpty(schema) && !StringUtils.isEmpty(interval)) {
                    revisarCorreo(schema, idArea);
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "erron on execute DownloadEmailJob", ex);
            } finally {
                try {
                    //schedule to run again after that interval
                    HelpDeskScheluder.scheduleRevisarCorreo(schema, idArea, Integer.valueOf(interval));
                } catch (SchedulerException ex) {
                    Logger.getLogger(DownloadEmailJob.class.getName()).log(Level.SEVERE, "ERROR TRYING TO scheduleRevisarCorreo", ex);
                }
            }
        }
    }

    private synchronized void revisarCorreo(String schema, String idArea) throws Exception, MailClientFactory.MailNotConfiguredException {

//        EntityManager em = createEntityManager(schema);
        EntityManagerFactory emf = createEntityManagerFactory();
        UserTransaction utx = UserTransactionHelper.lookupUserTransaction();
//        System.out.println(UserTransactionHelper.getUserTxLocation() + ":" + utx);//DEBUG

        JPAServiceFacade jpaController = new JPAServiceFacade(utx, emf, schema);
        ManagerCasos managerCasos = new ManagerCasos(jpaController);
        RulesEngine rulesEngine = new RulesEngine(jpaController);
        jpaController.setCasoChangeListener(rulesEngine);

//        JPAServiceFacade jpaController = createJpaController(schema);
//        ManagerCasos managerCasos = new ManagerCasos();
//        managerCasos.setJpaController(jpaController);
        Area area = jpaController.find(Area.class, idArea);
        if (area != null) {
            EmailClient mailClient = MailClientFactory.getInstance(schema, idArea);
            String emailSenderArea = area.getMailSmtpUser();
            String emailReceiverArea = area.getMailInboundUser();

            if (mailClient == null) {
                mailClient = MailClientFactory.createInstance(schema, area);
            }

            if (mailClient != null) {
                mailClient.connectStore();
                mailClient.openFolder("inbox");
                List<EmailMessage> messages = mailClient.getUnreadMessages();
                //List<EmailMessage> messages = mailClient.getAllMessages();
                List<BlackListEmail> blackList = (List<BlackListEmail>) jpaController.findAll(BlackListEmail.class);//findAll(BlackListEmail.class);
                HashMap<String, BlackListEmail> mapBlackList = new HashMap<String, BlackListEmail>();
                for (BlackListEmail blackListEmail : blackList) {
                    mapBlackList.put(blackListEmail.getEmailAddress(), blackListEmail);
                }

                if (ApplicationConfig.getInstance(schema).isAppDebugEnabled()) {
                    Log.createLogger(this.getClass().getName()).logDebug("Debug Email BlackList:" + mapBlackList);
                }

                for (EmailMessage emailMessage : messages) {
                    try {
                        if (!mapBlackList.containsKey(emailMessage.getFromEmail()) && (!emailMessage.getFromEmail().equalsIgnoreCase(emailSenderArea))
                                && (!emailMessage.getFromEmail().equalsIgnoreCase(emailReceiverArea))) {
                            final boolean casoIsCreated = managerCasos.crearCasoDesdeEmail(area, emailMessage);
                            if (casoIsCreated) {
                                mailClient.markReadMessage(emailMessage);
                                mailClient.deleteMessage(emailMessage);//TODO add a config, deleteMessagesAfterSuccessRead?
                            }
                        } else {
                            if (ApplicationConfig.getInstance(schema).isAppDebugEnabled()) {
                                Log.createLogger(this.getClass().getName()).logDebug("BLOCKED EMAIL FROM:" + emailMessage.getFromEmail());
                            }
                        }
                    } catch (MessagingException e) {
                        Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "MessagingException Error: No se pudo crear el caso. from: " + emailMessage.getFromEmail() + ", subject: " + emailMessage.getSubject(), e);
                    } finally {
                        mailClient.markReadMessage(emailMessage);
                    }

                }

                mailClient.closeFolder();
                mailClient.disconnectStore();

                if (ApplicationConfig.getInstance(schema).isAppDebugEnabled()) {
                    Log.createLogger(this.getClass().getName()).logDebug("Revisión de correo " + area + "exitósa: " + messages.size() + " mensajes leídos. Intancia: " + schema);
                }
            } else {
                throw new MailClientFactory.MailNotConfiguredException("No se puede enviar correos, favor comunicarse con el administrador para que configure la cuenta de correo asociada al Área.");
            }
        }

    }

    @Override
    protected String getGrupo() {
        return HelpDeskScheluder.GRUPO_CORREO;
    }

    @Override
    protected String getJobId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
