package com.itcs.helpdesk.util;

import com.itcs.commons.email.EmailClient;
import com.itcs.commons.email.impl.ExchangeEmailClientImpl;
import com.itcs.commons.email.impl.PopImapEmailClientImpl;
import com.itcs.helpdesk.persistence.entities.Area;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;

/**
 *
 * @author Jonathan
 */
public class MailClientFactory {

//    private static EmailClient instance;
    private static HashMap<String, HashMap<String, EmailClient>> clients = new HashMap<String, HashMap<String, EmailClient>>();

    /**
     * This method creates an instance of EmailClient based on the configuration
     * passed.
     *
     * @param props email connection properties
     * @param useJNDI if you dont want to use properties but a jndi Java Mail
     * Session
     * @param jndiName the JNDI NAME of Mail Session resource
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static EmailClient createInstance(String schema, Area a) throws FileNotFoundException, IOException {
//        if (instance == null) {
        EmailClient instance = null;

//        String serverType = a.getMailServerType();//props.getProperty(ApplicationConfig.MAIL_SERVER_TYPE);
        if (a.getMailServerType() != null && a.getMailServerType().equals(Email.POP_IMAP)) {
//            boolean useJNDI = Boolean.valueOf(a.getMailUseJndi());
            if (a.getMailUseJndi()) {
                instance = new PopImapEmailClientImpl(a.getMailSessionJndiname());//props.getProperty(ApplicationConfig.MAIL_SESSION_JNDINAME));
            } else {
                instance = new PopImapEmailClientImpl(ApplicationConfig.generateEmailPropertiesFromArea(a));
            }
        } else if (a.getMailServerType() != null && a.getMailServerType().equals(Email.EXCHANGE)) {
            instance = new ExchangeEmailClientImpl(ApplicationConfig.generateEmailPropertiesFromArea(a));
        }
//        }
        if (!clients.containsKey(schema)) {
            clients.put(schema, new HashMap<String, EmailClient>());
        }
        clients.get(schema).put(a.getIdArea(), instance);
//       clients.put(a.getIdArea(), instance);
        System.out.println("createInstance clients:"+clients);
        return instance;
    }

    /**
     * @return the instance
     */
    public static EmailClient getInstance(String schema, String idArea) {
        System.out.println("getInstance("+schema +","+idArea+") clients:"+clients);
        if (!StringUtils.isEmpty(schema) && !StringUtils.isEmpty(idArea) && clients != null && !clients.isEmpty() && clients.containsKey(schema) ) {
            if (clients.get(schema) != null && !clients.get(schema).isEmpty() && clients.get(schema).containsKey(idArea)) {
                return clients.get(schema).get(idArea);
            }
        }
        // throw new MailNotConfiguredException("No se puede enviar correos, favor comunicarse con el administrador para que configure la cuenta de correo asociada al Área.");
        return null;
    }

    public static class MailNotConfiguredException extends Exception {

        public MailNotConfiguredException(String message, Throwable cause) {
            super(message, cause);
        }

        public MailNotConfiguredException(Throwable cause) {
            super(cause);
        }

        public MailNotConfiguredException(String message) {
            super(message);
        }

    }
}
