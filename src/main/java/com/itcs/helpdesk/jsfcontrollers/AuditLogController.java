package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.FiltroAcceso;
import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.AuditLog;
import com.itcs.helpdesk.persistence.utils.vo.AuditLogVO;
import com.itcs.helpdesk.util.Log;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.ListDataModel;
import org.primefaces.model.SelectableDataModel;


@ManagedBean(name = "auditLogController")
@SessionScoped
public class AuditLogController extends AbstractManagedBean<AuditLog> implements Serializable {

    @ManagedProperty(value = "#{filtroAcceso}")
    private FiltroAcceso filtroAcceso;
//    private AuditLog current;
//    private AuditLog[] selectedItems;
   
    private int selectedItemIndex;
    private Date fechaInicio;
    private Date fechaTermino;

    /*
     * Elementos para las Alertas
     */
    private AuditLogVO alerta;
    //private List<AuditLog> alertas = null;
    boolean isAlerts = false;

    public AuditLogController() {
        super(AuditLog.class);
    }

    public AuditLogVO getAlerta() {
        return alerta;
    }

    public void setAlerta(AuditLogVO alerta) {
        this.alerta = alerta;
    }

    /*
     * public DataModel getAlertas() { if(alertas == null){ if(alerta == null){
     * alerta = new AuditLogVO(); } FiltroAcceso filtro = new FiltroAcceso();
     * if(!filtro.verificaAccesoAFuncion(EnumFunciones.FILTRO_OWNER)){
     * alerta.setIdUser(((UserSessionBean)JsfUtil.getManagedBean("UserSessionBean")).getCurrent());
     * }
     *
     * alertas = getJpaController().findAlertsEntities(alerta, false); return
     * new AuditLogDataModel(alertas); } return new AuditLogDataModel(alertas);
     * }
     */
//    public AuditLog getSelected() {
//        if (current == null) {
//            current = new AuditLog();
//            selectedItemIndex = -1;
//        }
//        return current;
//    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Date fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Date getFechaTermino() {
        return fechaTermino;
    }

    public void setFechaTermino(Date fechaTermino) {
        this.fechaTermino = fechaTermino;
    }



    public String prepareList() {
        pagination = null;
        alerta = new AuditLogVO();
        recreateModel();
        isAlerts = false;
        return "/script/audit_log/List";
    }

//    public String prepareView() {
//        if (getSelectedItems().length != 1) {
//            JsfUtil.addSuccessMessage("Es requerido que seleccione una fila para visualizar.");
//            return "";
//        } else {
//            current = getSelectedItems()[0];
//        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
//        return "/script/audit_log/View";
//    }

    public String prepareCreate() {
        current = new AuditLog();
        selectedItemIndex = -1;
        return "/script/audit_log/Create";
    }

    public String create() {
        try {
            getJpaController().persist(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("AuditLogCreated"));
            return prepareCreate();
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

//    public String prepareEdit() {
//        if (getSelectedItems().length != 1) {
//            JsfUtil.addSuccessMessage("Se requiere que seleccione una fila para editar.");
//            return "";
//        } else {
//            current = getSelectedItems()[0];
//        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
//        return "/script/audit_log/Edit";
//    }

    public String update() {
        try {
            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("AuditLogUpdated"));
            return "/script/audit_log/View";
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
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
        return "/script/audit_log/List";
    }

//    public String destroySelected() {
//
//        if (getSelectedItems().length <= 0) {
//            return "";
//        } else {
//            for (int i = 0; i < getSelectedItems().length; i++) {
//                current = getSelectedItems()[i];
//                performDestroy();
//            }
//        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
//        recreateModel();
//        return "/script/audit_log/List";
//    }

//    public String destroyAndView() {
//        performDestroy();
//        recreateModel();
//        updateCurrentItem();
//        if (selectedItemIndex >= 0) {
//            return "/script/audit_log/View";
//        } else {
//            // all items were removed - go back to list
//            recreateModel();
//            return "/script/audit_log/List";
//        }
//    }

    public String filterLogList() {
        try {
            if (getAlerta().getIdLogStr().isEmpty()) {
                alerta.setIdCaso(null);
            } else {
                alerta.setIdCaso(Long.parseLong((String) getAlerta().getIdLogStr()));
            }
            recreateModel();
            pagination = null;
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        }

        return "/script/audit_log/List";
    }

    public String filterAlertList() {
        try {
            if (getAlerta().getIdLogStr().isEmpty()) {
                alerta.setIdCaso(null);
            } else {
                alerta.setIdCaso(Long.parseLong((String) getAlerta().getIdLogStr()));
            }
            pagination = null;
            items = null;
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        }

        return "/script/audit_log/Alert";
    }

    public String alertList() {
        try {
            alerta = new AuditLogVO();
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.add(Calendar.MONTH, -1);
            cal2.add(Calendar.DAY_OF_YEAR, 1);
            alerta.setFechaInicio(cal1.getTime());
            alerta.setFechaFin(cal2.getTime());
            //alertas = null;
            pagination = null;
            items = null;
            isAlerts = true;
            //getJpaController().findAlertsEntities(null)
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return "/script/audit_log/Alert";
    }

    private void performDestroy() {
        try {
            getJpaController().remove(AuditLog.class, current.getIdLog());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("AuditLogDeleted"));
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

//    private void updateCurrentItem() {
//
//        if (alerta == null) {
//            alerta = new AuditLogVO();
//        }
////        if (!filtroAcceso.verificaAccesoAFuncion(EnumFunciones.FILTRO_OWNER)) {
////            alerta.setIdUser(((UserSessionBean) JsfUtil.getManagedBean("UserSessionBean")).getCurrent());
////        }
//
//        int count;
//        if (isAlerts) {
//            count = getJpaController().getAuditLogCount(alerta, false);
//        } else {
//            count = getJpaController().getAuditLogCount(alerta, true);
//        }
//
//        if (selectedItemIndex >= count) {
//            // selected index cannot be bigger than number of items:
//            selectedItemIndex = count - 1;
//            // go to previous page if last page disappeared:
//            if (pagination.getPageFirstItem() >= count) {
//                pagination.previousPage();
//            }
//        }
//        if (selectedItemIndex >= 0) {
//            current = getJpaController().findAuditLogEntities(1, selectedItemIndex, alerta, true).get(0);
//        }
//    }

//    public DataModel getItems() {
//        if (items == null) {
//            items = getPagination().createPageDataModel();
//        }
//        return items;
////        Iterator iter = items.iterator();
////        List<AuditLog> listOfAuditLog = new ArrayList<AuditLog>();
////        while (iter.hasNext()) {
////            listOfAuditLog.add((AuditLog) iter.next());
////        }
////
////        return new AuditLogDataModel(listOfAuditLog);
//    }

//    /**
//     * @return the selectedItems
//     */
//    public AuditLog[] getSelectedItems() {
//        return selectedItems;
//    }
//
//    /**
//     * @param selectedItems the selectedItems to set
//     */
//    public void setSelectedItems(AuditLog[] selectedItems) {
//        this.selectedItems = selectedItems;
//    }

//    private void recreateModel() {
//        //alertas = null;
//        items = null;
//    }
//
//    public String next() {
//        getPagination().nextPage();
//        recreateModel();
//        return "";
//    }
//
//    public String previous() {
//        getPagination().previousPage();
//        recreateModel();
//        return "";
//    }
//
//    public String last() {
//        getPagination().lastPage();
//        recreateModel();
//        return "";
//    }
//
//    public String first() {
//        getPagination().firstPage();
//        recreateModel();
//        return "";
//    }

//    @Override
//    public SelectItem[] getItemsAvailableSelectMany() {
//        if (alerta == null) {
//            alerta = new AuditLogVO();
//        }
////        if (!filtroAcceso.verificaAccesoAFuncion(EnumFunciones.FILTRO_OWNER)) {
////            alerta.setIdUser(((UserSessionBean) JsfUtil.getManagedBean("UserSessionBean")).getCurrent());
////        }
//        return JsfUtil.getSelectItems(getJpaController().findAuditLogEntities(alerta), false);
//    }
//
//    @Override
//    public SelectItem[] getItemsAvailableSelectOne() {
//        if (alerta == null) {
//            alerta = new AuditLogVO();
//        }
////        if (!filtroAcceso.verificaAccesoAFuncion(EnumFunciones.FILTRO_OWNER)) {
////            alerta.setIdUser(((UserSessionBean) JsfUtil.getManagedBean("UserSessionBean")).getCurrent());
////        }
//        return JsfUtil.getSelectItems(getJpaController().findAuditLogEntities(alerta), true);
//
//
//    }

    /**
     * @param filtroAcceso the filtroAcceso to set
     */
    public void setFiltroAcceso(FiltroAcceso filtroAcceso) {
        this.filtroAcceso = filtroAcceso;
    }

    @Override
    public Class getDataModelImplementationClass() {
       return AuditLogDataModel.class;
    }

    @FacesConverter(forClass = AuditLog.class)
    public static class AuditLogControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AuditLogController controller = (AuditLogController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "auditLogController");
            return controller.getJpaController().getAuditLogFindByIdLog(Long.valueOf(value));
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof AuditLog) {
                AuditLog o = (AuditLog) object;
                return getStringKey(o.getIdLog());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + AuditLogController.class.getName());
            }
        }
    }
}
class AuditLogDataModel extends ListDataModel<AuditLog> implements SelectableDataModel<AuditLog> {

    public AuditLogDataModel() {
        //nothing
    }

    public AuditLogDataModel(List<AuditLog> data) {
        super(data);
    }

    @Override
    public AuditLog getRowData(String rowKey) {
        List<AuditLog> listOfAuditLog = (List<AuditLog>) getWrappedData();

        for (AuditLog obj : listOfAuditLog) {
            if (obj.getIdLog().toString().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(AuditLog classname) {
        return classname.getIdLog();
    }
}
