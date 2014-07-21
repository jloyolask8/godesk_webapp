package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.BlackListEmail;
import com.itcs.helpdesk.util.Log;
import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.ListDataModel;
import org.primefaces.model.SelectableDataModel;

@ManagedBean
@SessionScoped
public class BlackListController extends AbstractManagedBean<BlackListEmail> implements Serializable {

    public BlackListController() {
        super(BlackListEmail.class);
    }

    public String prepareList() {
        recreateModel();
        return getListOutcomeNotAbstract();
    }

    public void prepareCreate() {
        current = new BlackListEmail();
    }

    public void prepareEdit(BlackListEmail o) {
        current = o;
    }

    public void create() {
        try {
            getJpaController().persist(current);
            JsfUtil.addSuccessMessage(getSelected().getClass().getSimpleName() + " guardado exitosamente.");
            recreateModel();
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
//            return null;
        }
    }

    public void update() {
        try {
            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(getSelected().getClass().getSimpleName() + " actualizado exitosamente.");
//            return "View";
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
//            return null;
        }
    }

    public void destroy(BlackListEmail o) {
        if (o != null) {
            try {
                getJpaController().remove(BlackListEmail.class, o.getEmailAddress());
                JsfUtil.addSuccessMessage(getSelected().getClass().getSimpleName() + " eliminado exitosamente.");
            } catch (Exception e) {
                Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
                JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            }
            recreateModel();
        }
    }

    public void destroySelected() {
        if (current != null) {
            performDestroy();
            recreateModel();
        }
    }

//    public String destroyAndViewNext() {
//        performDestroy();
//        recreateModel();
//        updateCurrentItem();
//        if (selectedItemIndex >= 0) {
//            return "View";
//        } else {
//            // all items were removed - go back to list
//            recreateModel();
//            return "List";
//        }
//    }
    private void performDestroy() {
        try {
            getJpaController().remove(BlackListEmail.class, current.getEmailAddress());
            JsfUtil.addSuccessMessage(getSelected().getClass().getSimpleName() + " eliminado exitosamente.");
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

    private String getListOutcomeNotAbstract() {
        return "/script/blackList/List";
    }

    @Override
    public Class getDataModelImplementationClass() {
       return BlackListEmailDataModel.class;
    }

    @FacesConverter(forClass = BlackListEmail.class)
    public static class BlackListEmailConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {

            if (value == null || value.length() == 0) {
                return null;
            }
            BlackListController controller = (BlackListController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "blackListController");
            return controller.getJpaController().find(BlackListEmail.class, getKey(value));
        }

        java.lang.String getKey(String value) {
            java.lang.String key;
            key = value;
            return key;
        }

        String getStringKey(java.lang.String value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof BlackListEmail) {
                BlackListEmail o = (BlackListEmail) object;
                return getStringKey(o.getEmailAddress());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + BlackListEmail.class.getName());
            }
        }
    }
}

class BlackListEmailDataModel extends ListDataModel<BlackListEmail> implements SelectableDataModel<BlackListEmail> {

    public BlackListEmailDataModel() {
        //nothing
    }

    public BlackListEmailDataModel(List<BlackListEmail> data) {
        super(data);
    }

    @Override
    public BlackListEmail getRowData(String rowKey) {
        List<BlackListEmail> listOfBlackListEmail = (List<BlackListEmail>) getWrappedData();

        for (BlackListEmail obj : listOfBlackListEmail) {
            if (obj.getEmailAddress().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(BlackListEmail classname) {
        return classname.getEmailAddress();
    }
}
