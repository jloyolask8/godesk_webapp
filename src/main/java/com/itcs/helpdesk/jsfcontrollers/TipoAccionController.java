package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.jsfcontrollers.util.PaginationHelper;
import com.itcs.helpdesk.persistence.entities.TipoAccion;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import org.primefaces.model.SelectableDataModel;


@ManagedBean(name = "tipoAccionController")
@SessionScoped
public class TipoAccionController extends AbstractManagedBean<TipoAccion> implements Serializable {

    private int selectedItemIndex;

    public TipoAccionController() {
        super(TipoAccion.class);
    }

    @Override
    public PaginationHelper getPagination() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public SelectItem[] getItemsAvailableSelectOneImplementingActionClass() {
        List<TipoAccion> lista = new LinkedList<TipoAccion>();
        final List<TipoAccion> findAll = (List<TipoAccion>)getJpaController().findAll(TipoAccion.class);
        for (TipoAccion tipoAccion : findAll) {
            if(tipoAccion.getImplementationClassName() != null){
                lista.add(tipoAccion);
            }
        }
        
        return JsfUtil.getSelectItems(lista, true);
    }

    @Override
    public Class getDataModelImplementationClass() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @FacesConverter(forClass = TipoAccion.class)
    public static class TipoAccionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TipoAccionController controller = (TipoAccionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "tipoAccionController");
            return controller.getJpaController().find(TipoAccion.class, getKey(value));
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
            if (object instanceof TipoAccion) {
                TipoAccion o = (TipoAccion) object;
                return getStringKey(o.getIdTipoAccion());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + TipoAccion.class.getName());
            }
        }
    }
}
class TipoAccionDataModel extends ListDataModel<TipoAccion> implements SelectableDataModel<TipoAccion> {

    public TipoAccionDataModel() {
        //nothing
    }

    public TipoAccionDataModel(List<TipoAccion> data) {
        super(data);
    }

    @Override
    public TipoAccion getRowData(String rowKey) {
        List<TipoAccion> listOfTipoAccion = (List<TipoAccion>) getWrappedData();

        for (TipoAccion obj : listOfTipoAccion) {
            if (obj.getIdTipoAccion().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(TipoAccion classname) {
        return classname.getIdTipoAccion();
    }
}
