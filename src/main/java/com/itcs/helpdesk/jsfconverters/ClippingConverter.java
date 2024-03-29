/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.jsfconverters;

import com.itcs.helpdesk.jsfcontrollers.CasoController;
import com.itcs.helpdesk.persistence.entities.Clipping;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author jonathan
 */
@FacesConverter(forClass = Clipping.class)
public class ClippingConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
            CasoController controller = (CasoController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "casoController");
            return controller.getJpaController().find(Clipping.class, getKey(value));
    }

    java.lang.Integer getKey(String value) {
        java.lang.Integer key;
        key = Integer.valueOf(value);
        return key;
    }

    String getStringKey(java.lang.Integer value) {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        return sb.toString();
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Clipping) {
            Clipping o = (Clipping) object;
            return getStringKey(o.getIdClipping());
        } else {
            throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + Clipping.class.getName());
        }
    }
}
