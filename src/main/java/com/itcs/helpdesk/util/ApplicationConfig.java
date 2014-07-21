/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.util;

import com.itcs.helpdesk.persistence.entities.Area;
import com.itcs.helpdesk.persistence.entityenums.EnumSettingsBase;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;

/**
 *
 * @author jonathan
 */
public class ApplicationConfig {
    private final String schema;
    private Properties configuration;
    private static final Map<String, ApplicationConfig> instances = new HashMap<String, ApplicationConfig>();
    private String ckEditorToolbar = "";

    public ApplicationConfig(String schema, Properties configuration) {
        this.schema = schema;
        this.configuration = configuration;
    }
   
    public static void loadTenantInstance(String schema, Properties configuration) {
        if (!StringUtils.isEmpty(schema)) {
//            if (!instances.containsKey(schema)) {
                ApplicationConfig instance = new ApplicationConfig(schema, configuration);
                instances.put(schema, instance);
//            }
        }
    }

    public static ApplicationConfig getInstance(String schema) {
        if (!StringUtils.isEmpty(schema)) {
            if (instances.containsKey(schema)) {
                return instances.get(schema);
            }
//            instance.setConfiguration(new Properties());
//            instance.loadFromBundleConfiguration();//Here i can change to load properties from the Database.
        }

        return null;
    }

    public static Properties generateEmailPropertiesFromArea(Area a) {

        Properties props = new Properties();

        try {
            // MAIL_DEBUG
            if (a.getMailDebug() != null) {
                props.put(MAIL_DEBUG, a.getMailDebug());
            } else {
                props.put(MAIL_DEBUG, Boolean.FALSE);
            }

            if (a.getMailServerType() != null && StringUtils.isNotEmpty(a.getMailServerType())) {
                if (a.getMailServerType().equalsIgnoreCase(EnumMailServerType.POPIMAP.getValue())) {

                    if (StringUtils.isNotEmpty(a.getMailStoreProtocol())) {
                        if (StringUtils.isNotEmpty(a.getMailInboundHost())) {
                            props.put(MAIL_PROTOCOL_HOST.replace("{0}", a.getMailStoreProtocol()), a.getMailInboundHost());
                        }

                        if (a.getMailInboundPort() != null) {
                            props.put(MAIL_PROTOCOL_PORT.replace("{0}", a.getMailStoreProtocol()), a.getMailInboundPort());
                        }

                        if (a.getMailInboundSslEnabled() != null) {
                            props.put(MAIL_PROTOCOL_SSL_ENABLED.replace("{0}", a.getMailStoreProtocol()), a.getMailInboundSslEnabled());
                        }

//                        if (StringUtils.isNotEmpty(a.getMailInboundUser())) {
//                            props.put(MAIL_PROTOCOL_USER.replace("{0}", a.getMailStoreProtocol()), a.getMailInboundUser());
//                        }
//
//                        if (StringUtils.isNotEmpty(a.getMailInboundPassword())) {
//                            props.put(MAIL_PROTOCOL_PASSWORD.replace("{0}", a.getMailStoreProtocol()), a.getMailInboundPassword());
//                        }
                    }

                } else if (a.getMailServerType().equalsIgnoreCase(EnumMailServerType.EXCHANGE.getValue())) {

                    if (a.getMailInboundUser() != null && StringUtils.isNotEmpty(a.getMailInboundUser())) {
                        props.put(Email.RECEIVER_USERNAME, a.getMailInboundUser());
                    }

                    if (a.getMailInboundPassword() != null && StringUtils.isNotEmpty(a.getMailInboundPassword())) {
                        props.put(Email.RECEIVER_PASSWORD, a.getMailInboundPassword());
                    }

                    if (a.getMailInboundHost() != null && StringUtils.isNotEmpty(a.getMailInboundHost())) {
                        props.put(Email.MAIL_EXCHANGE_SERVER, a.getMailInboundHost());
                    }
                    if (a.getDominioExchangeInbound() != null && StringUtils.isNotEmpty(a.getDominioExchangeInbound())) {
                        props.put(Email.MAIL_EXCHANGE_DOMAIN, a.getDominioExchangeInbound());
                    }

                }
            }

            if (a.getMailServerTypeSalida() != null && StringUtils.isNotEmpty(a.getMailServerTypeSalida())) {

                if (a.getMailServerTypeSalida().equalsIgnoreCase(EnumMailServerType.SMTP.getValue())) {

                    props.put(MAIL_SMTP_AUTH, "true");

                    if (a.getMailSmtpHost() != null && StringUtils.isNotEmpty(a.getMailSmtpHost())) {
                        props.put(MAIL_SMTP_HOST, a.getMailSmtpHost());
                    }
                    if (a.getMailSmtpPort() != null) {
                        props.put(MAIL_SMTP_PORT, a.getMailSmtpPort());
                    }
                    if (a.getMailSmtpUser() != null && StringUtils.isNotEmpty(a.getMailSmtpUser())) {
                        props.put(MAIL_SMTP_USER, a.getMailSmtpUser());
                    }
                    if (a.getMailSmtpPassword() != null && StringUtils.isNotEmpty(a.getMailSmtpPassword())) {
                        props.put(MAIL_SMTP_PASSWORD, a.getMailSmtpPassword());
                    }
                    if (a.getMailSmtpFrom() != null && StringUtils.isNotEmpty(a.getMailSmtpFrom())) {
                        props.put(MAIL_SMTP_FROM, a.getMailSmtpFrom());
                    }
                    if (a.getMailSmtpFromname() != null && StringUtils.isNotEmpty(a.getMailSmtpFromname())) {
                        props.put(MAIL_SMTP_FROMNAME, a.getMailSmtpFromname());
                    }

                    if (a.getMailSmtpSslEnable() != null && a.getMailSmtpSslEnable()) {
                        //use SSL
                        props.put(MAIL_SMTP_SSL_ENABLE, Boolean.TRUE);
                        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//                        props.put(MAIL_SMTP_AUTH, "true");

                        if (a.getMailSmtpSocketFactoryPort() != null) {
                            props.put(MAIL_SMTP_SOCKET_FACTORY_PORT, a.getMailSmtpSocketFactoryPort());
                        } else {
                            if (a.getMailSmtpPort() != null) {
                                props.put(MAIL_SMTP_SOCKET_FACTORY_PORT, a.getMailSmtpPort());
                            }

                        }

                    } else {
                        //check TLS
                        if (a.getMailTransportTls() != null && a.getMailTransportTls()) {
                            props.put(MAIL_TRANSPORT_TLS, Boolean.TRUE);

                        } else {
                            props.put(MAIL_TRANSPORT_TLS, Boolean.FALSE);
                        }
                    }

                    if (a.getMailSmtpConnectiontimeout() != null && a.getMailSmtpConnectiontimeout().intValue() != 0) {
                        props.put(MAIL_SMTP_CONNECTIONTIMEOUT, a.getMailSmtpConnectiontimeout());
                    }
                    if (a.getMailSmtpTimeout() != null && a.getMailSmtpTimeout().intValue() != 0) {
                        props.put(MAIL_SMTP_TIMEOUT, a.getMailSmtpTimeout());
                    }

                    if (a.getMailTransportProtocol() != null && StringUtils.isNotEmpty(a.getMailTransportProtocol())) {
                        props.put(MAIL_TRANSPORT_PROTOCOL, a.getMailTransportProtocol());
                    }

                    if (a.getMailStoreProtocol() != null && StringUtils.isNotEmpty(a.getMailStoreProtocol())) {
                        props.put(MAIL_STORE_PROTOCOL, a.getMailStoreProtocol());
                    }

                } else if (a.getMailServerTypeSalida().equalsIgnoreCase(EnumMailServerType.EXCHANGE.getValue())) {

                    if (a.getMailInboundUser() != null && StringUtils.isNotEmpty(a.getMailInboundUser())) {
                        props.put(Email.SENDER_USERNAME, a.getMailInboundUser());
                    }

                    if (a.getMailInboundPassword() != null && StringUtils.isNotEmpty(a.getMailInboundPassword())) {
                        props.put(Email.SENDER_PASSWORD, a.getMailInboundPassword());
                    }

                    if (a.getMailInboundHost() != null && StringUtils.isNotEmpty(a.getMailInboundHost())) {
                        props.put(Email.MAIL_EXCHANGE_SERVER, a.getMailInboundHost());
                    }
                    if (a.getDominioExchangeInbound() != null && StringUtils.isNotEmpty(a.getDominioExchangeInbound())) {
                        props.put(Email.MAIL_EXCHANGE_DOMAIN, a.getDominioExchangeInbound());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.logDebug("generateEmailPropertiesFromArea():" + props);

        return props;

    }

//    public void loadConfiguration(Properties p) {
////        configuration = new Properties();
//        configuration.putAll(p);
////        configuration = new Properties();
////        configuration.putAll(EntityClassReflector.getPropertiesFromEntity(appConfig));
////        configuration.putAll(EntityClassReflector.getPropertiesFromEntity(emailConfig));
////        getInstance().setConfiguration(configuration);
//        logger.logDebug(p);
//    }

//    private void loadFromBundleConfiguration() {
//        ResourceBundle configBundle = ResourceBundle.getBundle("/Config");
//        configuration = new Properties();
//        Enumeration<String> keys = configBundle.getKeys();
//        while (keys.hasMoreElements()) {
//            String key = keys.nextElement();
//            configuration.put(key, configBundle.getString(key));
//        }
//
//    }
    
    public enum EnumMailServerType {

        EXCHANGE("EXCHANGE"),
        POPIMAP("POPIMAP"),
        SMTP("SMTP");
        private final String value;

        private EnumMailServerType(String value) {
            this.value = value;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }
    }
    private static Log logger = Log.createLogger(ApplicationConfig.class.getName());
    private static final String MAIL_DEBUG = "mail.debug";
    private static final String MAIL_SMTP_HOST = "mail.smtp.host";
    private static final String MAIL_SMTP_PORT = "mail.smtp.port";
    private static final String MAIL_SMTP_USER = "mail.smtp.user";
    private static final String MAIL_SMTP_PASSWORD = "mail.smtp.password";
    private static final String MAIL_SMTP_FROM = "mail.smtp.from";
    private static final String MAIL_SMTP_FROMNAME = "mail.smtp.fromname";
    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    private static final String MAIL_SMTP_SSL_ENABLE = "mail.smtp.ssl.enable";
    private static final String MAIL_SMTP_SOCKET_FACTORY_PORT = "mail.smtp.socketFactory.port";
    private static final String MAIL_SMTP_CONNECTIONTIMEOUT = "mail.smtp.connectiontimeout";
    private static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
    private static final String MAIL_TRANSPORT_TLS = "mail.smtp.starttls.enable";
    //----------
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    //----------
    private static final String MAIL_PROTOCOL_HOST = "mail.{0}.host";
    private static final String MAIL_PROTOCOL_PORT = "mail.{0}.port";
//    private static final String MAIL_PROTOCOL_USER = "mail.{0}.user";
//    private static final String MAIL_PROTOCOL_PASSWORD = "mail.{0}.password";
    private static final String MAIL_PROTOCOL_SSL_ENABLED = "mail.{0}.ssl.enable";
    //----------
//    private static final String MAIL_IMAPS_HOST = "mail.imaps.host";
//    private static final String MAIL_IMAPS_PORT = "mail.imaps.port";
//    private static final String MAIL_IMAPS_USER = "mail.imaps.user";
//    private static final String MAIL_IMAPS_PASSWORD = "mail.imaps.password";
//    private static final String MAIL_IMAPS_SSL_ENABLED = "mail.imaps.ssl.enable";
    //------
    //----------  
    
    private String getProperty(String key) {
        return getConfiguration().getProperty(key);
    }

    private String getProperty(String key, String defaultValue) {
        return getConfiguration().getProperty(key, defaultValue);
    }

    public String getSaludoClienteHombre() {
        return getProperty(EnumSettingsBase.SALUDO_CLIENTE_HOMBRE.getAppSetting().getSettingKey());
    }

    public String getSaludoClienteMujer() {
        return getProperty(EnumSettingsBase.SALUDO_CLIENTE_MUJER.getAppSetting().getSettingKey());
    }

    public String getSaludoClienteUnknown() {
        return getProperty(EnumSettingsBase.SALUDO_CLIENTE_UNKNOWN.getAppSetting().getSettingKey());
    }

    public boolean isShowCompanyLogo() {
        boolean value = false;
        try {
            value = Boolean.valueOf(getProperty(EnumSettingsBase.SHOW_COMPANY_LOGO.getAppSetting().getSettingKey(), "false"));
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return value;
    }

//    public static String getAttachmentRuta() {
//        return getProperty(EnumSettingsBase.ATTACHMENT_RUTA.getAppSetting().getSettingKey());
//    }
    public String getHelpdeskTitle() {
        return getProperty(EnumSettingsBase.HELPDESK_TITLE.getAppSetting().getSettingKey());
    }

//    public String getCompanyLogo() {
//        return getProperty(EnumSettingsBase.COMPANY_LOGO_ID_ATTACHMENT.getAppSetting().getSettingKey());
//    }

    public int getCompanyLogoSize() {
        int value = 100;
        try {
            value = Integer.valueOf(getProperty(EnumSettingsBase.COMPANY_LOGO_SIZE.getAppSetting().getSettingKey(), "100"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return value;
    }

    public String getCompanyName() {
        return getProperty(EnumSettingsBase.COMPANY_NAME.getAppSetting().getSettingKey());
    }

    public String getNotificationSubjectText() {
        return getProperty(EnumSettingsBase.NOTIFICATION_SUBJECT_TEXT.getAppSetting().getSettingKey());
    }

    public String getNotificationBodyText() {
        return getProperty(EnumSettingsBase.NOTIFICATION_BODY_TEXT.getAppSetting().getSettingKey());
    }

    public String getNotificationClientSubjectText() {
        return getProperty(EnumSettingsBase.NOTIFICATION_UPDATE_CLIENT_SUBJECT_TEXT.getAppSetting().getSettingKey());
    }

    public String getNotificationClientBodyText() {
        return getProperty(EnumSettingsBase.NOTIFICATION_UPDATE_CLIENT_BODY_TEXT.getAppSetting().getSettingKey());
    }

    public String getProductDescription() {
        return getProperty(EnumSettingsBase.PRODUCT_DESCRIPTION.getAppSetting().getSettingKey());
    }

    public String getProductComponentDescription() {
        return getProperty(EnumSettingsBase.PRODUCT_COMP_DESCRIPTION.getAppSetting().getSettingKey());
    }

    public String getProductSubComponentDescription() {
        return getProperty(EnumSettingsBase.PRODUCT_SUBCOMP_DESCRIPTION.getAppSetting().getSettingKey());
    }

    public boolean isSendNotificationOnTransfer() {
        boolean value = true;
        try {
            value = Boolean.valueOf(getProperty(EnumSettingsBase.SEND_NOTIFICATION_ON_TRANSFER.getAppSetting().getSettingKey(), "true"));
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return value;
    }

    public boolean isAppDebugEnabled() {
        boolean value = false;
        try {
            value = Boolean.valueOf(getProperty(EnumSettingsBase.DEBUG_ENABLED.getAppSetting().getSettingKey(), "false"));
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return value;
    }

    public boolean isSendGroupNotifOnNewCaseEnabled() {
        boolean value = false;
        try {
            value = Boolean.valueOf(getProperty(EnumSettingsBase.SEND_GROUP_NOTIFICATION_ON_NEW_CASE.getAppSetting().getSettingKey(), "false"));
            if (isAppDebugEnabled()) {
                System.out.println("SEND_GROUP_NOTIFICATION_ON_NEW_CASE:" + value);
            }
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return value;
    }

    public boolean isRealTimeNotifToAgentsEnabled() {
        boolean value = false;
        try {
            value = Boolean.valueOf(getProperty(EnumSettingsBase.REAL_TIME_NOTIF_TO_AGENTS_ENABLED.getAppSetting().getSettingKey(), "false"));
            if (isAppDebugEnabled()) {
                System.out.println("isRealTimeNotifToAgentsEnabled:" + value);
            }
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return value;
    }

    public boolean isRealTimeNotifToCustomerEnabled() {
        boolean value = false;
        try {
            value = Boolean.valueOf(getProperty(EnumSettingsBase.REAL_TIME_NOTIF_TO_CUSTOMER_ENABLED.getAppSetting().getSettingKey(), "false"));
            if (isAppDebugEnabled()) {
                System.out.println("isRealTimeNotifToCustomerEnabled:" + value);
            }
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return value;
    }

//    private static String getMailServerType() {
//        return getProperty(MAIL_SERVER_TYPE);
//    }
//
//    private static String getMailSessionJndiName() {
//        return getProperty(MAIL_SESSION_JNDINAME);
//    }
//
//    private static String getTextoRespuestaCaso() {
//        return getProperty(TEXTO_RESP_CASO);
//    }
//
//    private static String getTextoRespuestaAutomatica() {
//        return getProperty(TEXTO_RESP_AUTOMATICA);
//    }
//
//    private static String getSubjectRespuestaAutomatica() {
//        return getProperty(SUBJECT_RESP_AUTOMATICA);
//    }
//    private static boolean getEmailAcuseDeRecibo() {
//        boolean value = false;
//        try {
//            value = Boolean.valueOf(getProperty(ApplicationConfig.EMAIL_ACUSEDERECIBO, "false"));
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, null, e);
//        }
//        return value;
//    }
//
//    private static boolean isEmailEnabled() {
//        boolean value = false;
//        try {
//            value = Boolean.valueOf(getProperty(EMAIL_ENABLED, "false"));
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, null, e);
//        }
//        return value;
//    }
//
//    private static boolean isJndiEnabled() {
//        boolean value = false;
//        try {
//            value = Boolean.valueOf(getProperty(MAIL_USE_JNDI, "false"));
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, null, e);
//        }
//        return value;
//    }
//    /**
//     * @return the EMAIL_FRECUENCIA
//     */
//    private static int getEmailFrecuencia() {
//        int value = 10;
//        try {
//            value = Integer.valueOf(getProperty(EMAIL_FRECUENCIA));
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, null, e);
//        }
//        return value;
//    }
    /**
     * @return the configuration
     */
    public Properties getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the configuration to set
     */
    private void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }
}
