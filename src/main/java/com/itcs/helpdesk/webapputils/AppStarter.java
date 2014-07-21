/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.webapputils;

import com.itcs.commons.email.impl.PopImapEmailClientImpl;
import com.itcs.helpdesk.persistence.entities.AppSetting;
import com.itcs.helpdesk.persistence.entities.Area;
import com.itcs.helpdesk.persistence.jpa.AppSettingJpaController;
import com.itcs.helpdesk.persistence.jpa.AreaJpaController;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import com.itcs.helpdesk.util.ApplicationConfig;
import com.itcs.helpdesk.util.AutomaticOpsExecutor;
import com.itcs.helpdesk.quartz.HelpDeskScheluder;
import com.itcs.helpdesk.util.Log;
import com.itcs.helpdesk.util.MailClientFactory;
import com.itcs.helpdesk.util.ManagerCasos;
import com.itcs.helpdesk.util.RulesEngine;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import org.quartz.SchedulerException;

/**
 *
 * @author jorge
 */
public class AppStarter implements ServletContextListener {

    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "helpdeskPU")
    private EntityManagerFactory emf;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            //DOMConfigurator.configureAndWatch(filename, LOG_WATCH_PERIOD);
            inicializar();
        } catch (Exception ex) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            HelpDeskScheluder.stop();
            PopImapEmailClientImpl.getExecutorService().shutdown();
            Log.createLogger(this.getClass().getName()).logInfo("stopped HelpDesk Scheluder");
        } catch (SchedulerException ex) {
            Logger.getLogger(AppStarter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * we need to initialize many contexts now. One Context for each tenant.
     *
     * @throws SchedulerException
     */
    private void inicializar() throws SchedulerException {
        Log.createLogger(this.getClass().getName()).logInfo("Inicializando contextos");

        try {
//            EntityManager em = emf.createEntityManager();
//            Connection connection = em.unwrap(Connection.class);
            Connection connection = lookupConnection();
            DatabaseMetaData metaData = connection.getMetaData();

            ResultSet res = metaData.getSchemas();
            System.out.println("List of schemas: ");
            while (res.next()) {
                String schema = res.getString(1);
                System.out.println("schema:" + schema);
                if (schema != null && !schema.equalsIgnoreCase("public") && !schema.equalsIgnoreCase("information_schema") && !schema.equalsIgnoreCase("pg_catalog")) {//, 
                    initTenant(schema);
                }
            }
            res.close();

            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(AppStarter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void initTenant(String schema) {
        //loop over schemas in db. and do this
        Logger.getLogger(AppStarter.class.getName()).log(Level.INFO, "Loading tenant {0}...", schema);

        try {

            JPAServiceFacade jpaController = new JPAServiceFacade(utx, emf, schema);
            RulesEngine rulesEngine = new RulesEngine(jpaController);
            jpaController.setCasoChangeListener(rulesEngine);

            AutomaticOpsExecutor autoOpsExec = new AutomaticOpsExecutor(jpaController);
            autoOpsExec.verificaDatosBase();

            //1. Load Settings FROM APP_SETTING Table
            AppSettingJpaController appSettingJpaController = new AppSettingJpaController(utx, emf, schema);//TODO Change schema dynamic
            List<AppSetting> appSettings = appSettingJpaController.findAppSettingEntities();
            Properties props = new Properties();
            for (AppSetting appSetting : appSettings) {
                if (appSetting != null) {
                    if (appSetting.getSettingKey() != null) {
                        if (appSetting.getSettingValue() != null) {
                            props.put(appSetting.getSettingKey(), appSetting.getSettingValue());
                        } else {
                            props.put(appSetting.getSettingKey(), "");
                        }
                    }
                }
            }

            ApplicationConfig.loadTenantInstance(schema, props);

            //done
            //2. Load Areas, and configuration of each Area. 
            //Each Area will have its own email configuration to send and read emails.
            AreaJpaController areaJpaController = new AreaJpaController(utx, emf, schema);
            List<Area> areas = areaJpaController.findAreaEntities();
            if (areas
                    != null && !areas.isEmpty()) {
                for (Area a : areas) {
                    if (a.getEmailEnabled() != null && a.getEmailEnabled()) {
                        try {
                            //Email Enabled is false by default, so if its not configured yet, we schedule nothing, this means when we change the config to true, we must re-schedule.
//                            AutomaticMailScheduler mailExec = new AutomaticMailScheduler(a, new JPAServiceFacade(utx, emf, schema));
//                            mailExec.agendarRevisarCorreo();
                            MailClientFactory.createInstance(jpaController.getSchema(), a);
                            HelpDeskScheluder.scheduleRevisarCorreo(schema, a.getIdArea(), a.getEmailFrecuencia());

                        } catch (SchedulerException ex) {
                            Logger.getLogger(AppStarter.class.getName()).log(Level.SEVERE, "No se pudo inicializar La revision de correo del area " + a.getIdArea(), ex);
                        }
                    }
                }

            }
            //Done!
            //now is not necessary as we persisted the triggers in the DB.
//            autoOpsExec.agendarAlertasForAllCasos(schema);

            Logger.getLogger(AppStarter.class.getName()).log(Level.INFO, "Tenant {0} loaded OK.", schema);

        } catch (Exception ex) {
            Logger.getLogger(AppStarter.class.getName()).log(Level.SEVERE, "Tenant " + schema + " loading ERROR.", ex);
        }

    }

    static Connection lookupConnection() {
        String DATASOURCE_CONTEXT = "jdbc/godesk_db_ds";

        Connection result = null;
        try {
            Context initialContext = new InitialContext();
            DataSource datasource = (DataSource) initialContext.lookup(DATASOURCE_CONTEXT);
            if (datasource != null) {
                result = datasource.getConnection();
                System.out.println("DataSource Connection:" + result);
            } else {
                System.out.println("Failed to lookup datasource.");
            }
        } catch (Exception ex) {
            System.out.println("Cannot get connection: " + ex);
        }
        return result;
    }
}
