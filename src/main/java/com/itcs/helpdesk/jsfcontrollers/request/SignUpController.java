/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.jsfcontrollers.request;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.UserTransaction;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jonathan
 */
@ManagedBean
@RequestScoped
public class SignUpController {

    protected UserTransaction utx = null;
    @PersistenceUnit(unitName = "helpdeskPU")
    protected EntityManagerFactory emf = null;

    private String name;
    private String email;
    private String username;
    private String password;
    private String companyName;

    /**
     * Creates a new instance of SignUpController
     */
    public SignUpController() {
    }

    /**
     * rapid code this needs to be refactored
     *
     * @return
     */
    public String signup() {
        if (!StringUtils.isEmpty(companyName)) {
            EntityManager em = null;
            try {
                utx.begin();
                em = emf.createEntityManager();
                em.createNativeQuery("SELECT clone_schema('public','" + companyName + "')");

                utx.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the companyName
     */
    public String getCompanyName() {
        return companyName;
    }

    /**
     * @param companyName the companyName to set
     */
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

}
