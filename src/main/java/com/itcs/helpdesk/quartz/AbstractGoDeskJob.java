/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.quartz;

import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import com.itcs.helpdesk.util.RulesEngine;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;
import org.eclipse.persistence.config.EntityManagerProperties;
import org.quartz.SchedulerException;
import org.quartz.ee.jta.UserTransactionHelper;

/**
 *
 * @author jonathan
 */
public abstract class AbstractGoDeskJob {

    public static final String ID_TENANT = "schema";
    public static final String ID_AREA = "idArea";
    public static final String INTERVAL_SECONDS = "intervalInSeconds";
    
    protected abstract String getGrupo();
    protected abstract String getJobId();

//    @PersistenceUnit(unitName = "helpdeskPU")
//    protected EntityManagerFactory emf = null;
//    @Resource
//    protected UserTransaction utx = null;
//
//    public EntityManager createEntityManager(String schema) throws IllegalStateException{
//        if (!StringUtils.isEmpty(schema)) {
//            
////            return new JPAServiceFacade(utx, emf, schema);
//        } else {
//            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "schema is null or empty!", null);
//            throw new IllegalStateException("schema is null or empty!");
//        }
//    }
    protected EntityManager createEntityManager(String schema) {
        EntityManagerFactory emf = createEntityManagerFactory();
        EntityManager em = emf.createEntityManager();
        em.setProperty(EntityManagerProperties.MULTITENANT_PROPERTY_DEFAULT, schema);
        return em;
    }
    
    protected JPAServiceFacade getJpaController(String schema) throws SchedulerException{
         EntityManagerFactory emf = createEntityManagerFactory();
        UserTransaction utx = UserTransactionHelper.lookupUserTransaction();
//        System.out.println(UserTransactionHelper.getUserTxLocation() + ":" + utx);//DEBUG

        JPAServiceFacade jpaController = new JPAServiceFacade(utx, emf, schema);
//        ManagerCasos managerCasos = new ManagerCasos(jpaController);
        RulesEngine rulesEngine = new RulesEngine(jpaController);
        jpaController.setCasoChangeListener(rulesEngine);
        return jpaController;
    }
    
    protected EntityManagerFactory createEntityManagerFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("helpdeskPU");
//        EntityManager em = emf.createEntityManager();
//        em.setProperty(EntityManagerProperties.MULTITENANT_PROPERTY_DEFAULT, schema);
        return emf;
    }
    
 

}
