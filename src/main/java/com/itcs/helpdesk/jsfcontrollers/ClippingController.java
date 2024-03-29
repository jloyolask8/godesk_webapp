package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.jsfcontrollers.util.UserSessionBean;
import com.itcs.helpdesk.persistence.entities.Clipping;
import com.itcs.helpdesk.util.ClippingsPlaceHolders;
import com.itcs.helpdesk.util.Constants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;

@ManagedBean(name = "clippingController")
@SessionScoped
public class ClippingController extends AbstractManagedBean<Clipping> implements Serializable {

    private int selectedItemIndex;
    private Integer visibilityOption = 1;
    //!--Filter
//    private Vista vista;
    @ManagedProperty(value = "#{UserSessionBean}")
    protected UserSessionBean userSessionBean;
    
       /**
     * @param userSessionBean the userSessionBean to set
     */
    public void setUserSessionBean(UserSessionBean userSessionBean) {
        this.userSessionBean = userSessionBean;
    }
    
    
     

    //--Filter
    public ClippingController() {
        super(Clipping.class);
    }

    public void saveListener() {
        //Well all happens behinde the scene in jsf dude! 
    }
    
    public void handleAnyChangeEvent(){
        
    }
 
 
    public String prepareList() {
        recreateModel();
        return "/script/clipping/List";
    }

    public void prepareCreate() {
        current = new Clipping();
        selectedItemIndex = -1;
//        return "/script/clipping/Create";
    }

    public Integer determineVisibility(Clipping clipping) {
        if (clipping.getVisibleToAll()) {
            return Constants.VISIBILITY_ALL;
        } else {
            if (clipping.getIdArea() != null) {
                return Constants.VISIBILITY_AREA;
            } else if (clipping.getIdGrupo() != null) {
                return Constants.VISIBILITY_GRUPO;
            } else {
                return Constants.VISIBILITY_ALL;
            }
        }
    }

    public String create() {
        try {
            visibilityOption = determineVisibility(current);

            current.setIdUsuarioCreadaPor(userSessionBean.getCurrent());
            switch (visibilityOption) {
                case Constants.VISIBILITY_ALL: //Visible to all option
                    current.setIdArea(null);
                    current.setIdGrupo(null);
                    break;
                case Constants.VISIBILITY_GRUPO: // only Group option
                    current.setIdArea(null);
                    current.setVisibleToAll(false);
                    break;
                case Constants.VISIBILITY_AREA: //Only Area Option
                    current.setIdGrupo(null);
                    current.setVisibleToAll(false);
                    break;
                default: //Error

            }

            getJpaController().persist(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ClippingCreated"));
            return prepareList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareView() {
        current = (Clipping) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "/script/clipping/View";
    }

    public String prepareEdit() {
        current = (Clipping) getItems().getRowData();
        visibilityOption = determineVisibility(current);
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "/script/clipping/Edit";
    }

    public String update() {
        try {
            switch (visibilityOption) {
                case Constants.VISIBILITY_ALL: //Visible to all option
                    current.setIdArea(null);
                    current.setIdGrupo(null);
                    break;
                case Constants.VISIBILITY_GRUPO: // only Group option
                    current.setIdArea(null);
                    current.setVisibleToAll(false);
                    break;
                case Constants.VISIBILITY_AREA: //Only Area Option
                    current.setIdGrupo(null);
                    current.setVisibleToAll(false);
                    break;
                default:
                    break;//Error

            }
            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ClippingUpdated"));
            return null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (Clipping) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "/script/clipping/List";
    }

    public void selectItem() {
        current = (Clipping) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
    }

    public String destroySelected() {
        performDestroy();
        recreateModel();
        return "/script/clipping/List";
    }

    private void performDestroy() {
        try {
            getJpaController().remove(Clipping.class, current.getIdClipping());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ClippingDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

//    private void updateCurrentItem() {
//        int count = getJpaController().count(Clipping.class).intValue();
//        if (selectedItemIndex >= count) {
//            // selected index cannot be bigger than number of items:
//            selectedItemIndex = count - 1;
//            // go to previous page if last page disappeared:
//            if (pagination.getPageFirstItem() >= count) {
//                pagination.previousPage();
//            }
//        }
//        if (selectedItemIndex >= 0) {
//            current = getJpaController().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
//        }
//    }


    public List<Object> getAvailablePlaceHolders() {
        final ArrayList<Object> arrayList = new ArrayList<Object>(ClippingsPlaceHolders.getAvailablePlaceHolders());
        arrayList.add(ClippingsPlaceHolders.SALUDO_CLIENTE);
        return arrayList;
    }



    /**
     * @return the visibilityOption
     */
    public Integer getVisibilityOption() {
        return visibilityOption;
    }

    /**
     * @param visibilityOption the visibilityOption to set
     */
    public void setVisibilityOption(Integer visibilityOption) {
        this.visibilityOption = visibilityOption;
    }

    @Override
    public Class getDataModelImplementationClass() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @FacesConverter(forClass = Clipping.class)
    public static class ClippingControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ClippingController controller = (ClippingController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "clippingController");
            System.out.println("converting clipping id " + getKey(value).toString());
            final Clipping clip = controller.getJpaController().find(Clipping.class, getKey(value));
            System.out.println("Found " + clip);
            return clip;
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
}