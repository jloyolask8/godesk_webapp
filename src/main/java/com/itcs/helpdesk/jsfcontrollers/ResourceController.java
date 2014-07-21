package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.Resource;

import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.ListDataModel;
import org.primefaces.model.SelectableDataModel;

@ManagedBean(name = "resourceController")
@SessionScoped
public class ResourceController extends AbstractManagedBean<Resource> implements Serializable {

    private int selectedItemIndex;

    public ResourceController() {
        super(Resource.class);
    }

    public List<Resource> getAllItemsAvailableForSelect() {
        return (List<Resource>) getJpaController().findAll(Resource.class);
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        current = (Resource) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Resource();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            getJpaController().persist(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ResourceCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit() {
        current = (Resource) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ResourceUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (Resource) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
//        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getJpaController().remove(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ResourceDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

    @Override
    public Class getDataModelImplementationClass() {
        return ResourceDataModel.class;
    }

    @FacesConverter(forClass = Resource.class)
    public static class ResourceControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            System.out.println("ResourceControllerConverter.getAsObject");
            if (value == null || value.length() == 0) {
                return null;
            }
            ResourceController controller = (ResourceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "resourceController");
            return controller.getJpaController().find(Resource.class, getKey(value));
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
            System.out.println("ResourceControllerConverter.getAsString");
            if (object == null) {
                return null;
            }
            if (object instanceof Resource) {
                Resource o = (Resource) object;
                return getStringKey(o.getIdResource());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + Resource.class.getName());
            }
        }

    }

}

class ResourceDataModel extends ListDataModel<Resource> implements SelectableDataModel<Resource> {

    public ResourceDataModel() {
        //nothing
    }

    public ResourceDataModel(List<Resource> data) {
        super(data);
    }

    @Override
    public Resource getRowData(String rowKey) {
        List<Resource> listOfResource = (List<Resource>) getWrappedData();

        for (Resource obj : listOfResource) {
            if (obj.getIdResource().toString().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(Resource classname) {
        return classname.getIdResource();
    }
}
