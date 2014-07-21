/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.util;

import java.io.Serializable;

/**
 *
 * @author jonathan
 */
public class BackendEvent implements Serializable {
   
    public static final int TYPE_ASSIGNED_CASO = 1;
    public static final int TYPE_EDITED_CASO = 2;
    
    private int eventType;
    private Long idCaso;
    private String idUsuarioAssigned;//or old owner
    private String assigner;//or modifier
    

    public BackendEvent() {
    }

    /**
     * @return the eventType
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * @param eventType the eventType to set
     */
    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    /**
     * @return the idCaso
     */
    public Long getIdCaso() {
        return idCaso;
    }

    /**
     * @param idCaso the idCaso to set
     */
    public void setIdCaso(Long idCaso) {
        this.idCaso = idCaso;
    }

    /**
     * @return the idUsuarioAssigned
     */
    public String getIdUsuarioAssigned() {
        return idUsuarioAssigned;
    }

    /**
     * @param idUsuarioAssigned the idUsuarioAssigned to set
     */
    public void setIdUsuarioAssigned(String idUsuarioAssigned) {
        this.idUsuarioAssigned = idUsuarioAssigned;
    }

    /**
     * @return the assigner
     */
    public String getAssigner() {
        return assigner;
    }

    /**
     * @param assigner the assigner to set
     */
    public void setAssigner(String assigner) {
        this.assigner = assigner;
    }
    
    
    
}