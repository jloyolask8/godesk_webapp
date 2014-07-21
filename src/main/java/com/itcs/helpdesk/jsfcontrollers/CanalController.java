package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.Canal;
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


@ManagedBean(name = "canalController")
@SessionScoped
public class CanalController extends AbstractManagedBean<Canal> implements Serializable {

//    private Canal current;
//    private Canal[] selectedItems;
    private String mode;

    public CanalController() {
        super(Canal.class);
    }

//    public Canal getSelected() {
//        if (current == null) {
//            current = new Canal();
//        }
//        return current;
//    }

    public String prepareList() {
        recreateModel();
        return "/script/canal/List";
    }

    public void prepareView(Canal c) {
        current = c;
    }

    public void prepareCreate() {
        current = new Canal();
        mode = "Create";
    }

    public void prepareEdit(Canal c) {
        current = c;
        mode = "Edit";
    }

    public void create() {
        try {
            getJpaController().persist(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("CanalCreated"));
            recreateModel();
//            return prepareCreate();
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
//            return null;
        }
    }

    public void update() {
        try {
            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("CanalUpdated"));
//            return "View";
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
//            return null;
        }
    }

    public void destroy(Canal c) {
         if (c != null) {
            current = c;
            performDestroy();
            recreateModel();
        }   
    }

    public void destroySelected() {
        if (current != null) {
            performDestroy();
            recreateModel();
        }        
//        return "List";
    }

//    public String destroyAndView() {
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
            getJpaController().remove(Canal.class, current.getIdCanal());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("CanalDeleted"));
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

//    private void updateCurrentItem() {
//        int count = getJpaController().count(Canal.class).intValue();
//        if (selectedItemIndex >= count) {
//            // selected index cannot be bigger than number of items:
//            selectedItemIndex = count - 1;
//            // go to previous page if last page disappeared:
//            if (pagination.getPageFirstItem() >= count) {
//                pagination.previousPage();
//            }
//        }
//        if (selectedItemIndex >= 0) {
//            current = (Canal) getJpaController().queryByRange(Canal.class, 1, selectedItemIndex).get(0);
//        }
//    }


//    /**
//     * @return the selectedItems
//     */
//    public Canal[] getSelectedItems() {
//        return selectedItems;
//    }
//
//    /**
//     * @param selectedItems the selectedItems to set
//     */
//    public void setSelectedItems(Canal[] selectedItems) {
//        this.selectedItems = selectedItems;
//    }

//    public SelectItem[] getItemsAvailableSelectMany() {
//        return JsfUtil.getSelectItems(getJpaController().getCanalFindAll(), false);
//    }
//
//    public SelectItem[] getItemsAvailableSelectOne() {
//        return JsfUtil.getSelectItems(getJpaController().getCanalFindAll(), true);
//    }

    /**
     * @return the mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public Class getDataModelImplementationClass() {
        return CanalDataModel.class;
    }

    @FacesConverter(forClass = Canal.class)
    public static class CanalControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
          
            if (value == null || value.length() == 0) {
                return null;
            }
            CanalController controller = (CanalController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "canalController");
            return controller.getJpaController().find(Canal.class, getKey(value));
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
            if (object instanceof Canal) {
                Canal o = (Canal) object;
                return getStringKey(o.getIdCanal());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + CanalController.class.getName());
            }
        }
    }
}
class CanalDataModel extends ListDataModel<Canal> implements SelectableDataModel<Canal> {

    public CanalDataModel() {
        //nothing
    }

    public CanalDataModel(List<Canal> data) {
        super(data);
    }

    @Override
    public Canal getRowData(String rowKey) {
        List<Canal> listOfCanal = (List<Canal>) getWrappedData();

        for (Canal obj : listOfCanal) {
            if (obj.getIdCanal().equals(rowKey)) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public Object getRowKey(Canal classname) {
        return classname.getIdCanal();
    }
}