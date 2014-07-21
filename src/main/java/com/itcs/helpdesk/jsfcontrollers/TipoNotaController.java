package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.TipoNota;
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


@ManagedBean(name = "tipoNotaController")
@SessionScoped
public class TipoNotaController extends AbstractManagedBean<TipoNota> implements Serializable {

//    private TipoNota current;
//    private TipoNota[] selectedItems;
  
    private int selectedItemIndex;

    public TipoNotaController() {
        super(TipoNota.class);
    }

//    public TipoNota getSelected() {
//        if (current == null) {
//            current = new TipoNota();
//            selectedItemIndex = -1;
//        }
//        return current;
//    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareCreate() {
        current = new TipoNota();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            getJpaController().persist(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("TipoNotaCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String update() {
        try {
            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("TipoNotaUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        if (current == null) {
            return "";
        }
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    private void performDestroy() {
        try {
            getJpaController().remove(TipoNota.class, current.getIdTipoNota());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("TipoNotaDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }


    @Override
    public Class getDataModelImplementationClass() {
        return TipoNotaDataModel.class;
    }

    @FacesConverter(forClass = TipoNota.class)
    public static class TipoNotaControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TipoNotaController controller = (TipoNotaController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "tipoNotaController");
            return controller.getJpaController().find(TipoNota.class, getKey(value));
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
            if (object instanceof TipoNota) {
                TipoNota o = (TipoNota) object;
                return getStringKey(o.getIdTipoNota());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + TipoNota.class.getName());
            }
        }
    }
}
class TipoNotaDataModel extends ListDataModel<TipoNota> implements SelectableDataModel<TipoNota> {

    public TipoNotaDataModel() {
        //nothing
    }

    public TipoNotaDataModel(List<TipoNota> data) {
        super(data);
    }

    @Override
    public TipoNota getRowData(String rowKey) {
        List<TipoNota> listOfTipoNota = (List<TipoNota>) getWrappedData();

        for (TipoNota obj : listOfTipoNota) {
            if (obj.getIdTipoNota().toString().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(TipoNota classname) {
        return classname.getIdTipoNota();
    }
}
