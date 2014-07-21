/*
 * Abstract action to inherit from, An action is executed when a Rules applies to case.
 */
package com.itcs.helpdesk.rules;

import com.itcs.helpdesk.persistence.entities.Caso;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.eclipse.persistence.config.EntityManagerProperties;

/**
 *
 * @author jonathan
 */
public abstract class Action {

//    private JPAServiceFacade jpaController = null;
//    private ManagerCasos managerCasos;
    /**
     * config represents the parametros (saved in DB), it can be any String
     * representation of data that Action needs to execute.
     */
    private String config;

//     public Action(JPAServiceFacade jpaController, ManagerCasos managerCasos) {
//        this.jpaController = jpaController;
//        this.managerCasos = managerCasos;
//    }
    /**
     *
     * @param caso the caso being created, so i have a reference to update the
     * caso in case the action updates the caso.
     */
    public abstract void execute(final Caso caso, final String schema) throws ActionExecutionException;

    
    protected EntityManager createEntityManager(String schema) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("helpdeskPU");
        EntityManager em = emf.createEntityManager();
        em.setProperty(EntityManagerProperties.MULTITENANT_PROPERTY_DEFAULT, schema);
        return em;
    }
//    protected JPAServiceFacade getJpaController() {
//        return jpaController;
//    }
//
//    /**
//     * @return the managerCasos
//     */
//    protected ManagerCasos getManagerCasos() {
//        return managerCasos;
//    }
    /**
     * @return the config
     */
    public String getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(String config) {
        this.config = config;
    }

}
