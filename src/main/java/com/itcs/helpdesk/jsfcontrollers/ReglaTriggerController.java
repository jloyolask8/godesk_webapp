package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JPAFilterHelper;
import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.Accion;
import com.itcs.helpdesk.persistence.entities.Area;
import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.Categoria;
import com.itcs.helpdesk.persistence.entities.Condicion;
import com.itcs.helpdesk.persistence.entities.CustomField;
import com.itcs.helpdesk.persistence.entities.FiltroVista;
import com.itcs.helpdesk.persistence.entities.Grupo;
import com.itcs.helpdesk.persistence.entities.Prioridad;
import com.itcs.helpdesk.persistence.entities.ReglaTrigger;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.persistence.entities.Vista;
import com.itcs.helpdesk.persistence.entityenums.EnumFieldType;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAccion;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import com.itcs.helpdesk.persistence.utils.OrderBy;
import com.itcs.helpdesk.rules.actionsimpl.ScheduleJobAction;
import com.itcs.helpdesk.util.EmailStruct;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.ListDataModel;
import org.primefaces.model.SelectableDataModel;
import org.primefaces.model.TreeNode;

@ManagedBean(name = "reglaTriggerController")
@SessionScoped
public class ReglaTriggerController extends AbstractManagedBean<ReglaTrigger> implements Serializable {

//    private ReglaTrigger current;
//    private List<ReglaTrigger> reglaItems = null;
//    private transient PaginationHelper pagination;
    private int selectedItemIndex;
    //----logica de reglas
    private List<Accion> acciones = new ArrayList<Accion>();
    private Accion accionTemp = new Accion();
    //----fin logica de reglas
    private transient XStream xstream;
    private transient TreeNode catNodeSelected;
    private Grupo grupoTemp;
    private Area areaTemp;
    private Usuario usuarioTemp;
    private Prioridad prioridadTemp;
    private String customFieldKeyDates;
    private transient EmailStruct emailTemp;
//    private transient JPAFilterHelper filterHelper;
    private transient JPAFilterHelper filterHelper2;
    private Vista vistaCondicionesList;

    public ReglaTriggerController() {
        super(ReglaTrigger.class);
        xstream = new XStream();
    }

    public List<String> getCustomFieldDateItemsAvailable() {

        List<String> lista = new LinkedList<String>();

        final List<CustomField> findAll = (List<CustomField>) getJpaController().findAll(CustomField.class);
        for (CustomField customField : findAll) {
            if (customField.getFieldTypeId().equals(EnumFieldType.CALENDAR.getFieldType()) && customField.isCasoCustomField()) {
                lista.add(customField.getCustomFieldPK().getFieldKey());
            }
        }

        return lista;
    }

//    public void onReglaOrderEdit() {
////        Object oldValue = event.getOldValue();  
////        Object newValue = event.getNewValue();  
////          
////        if(newValue != null && !newValue.equals(oldValue)) {  
////        Collections.sort(()getItems().getWrappedData());
//        for (ReglaTrigger reglaTrigger : reglaItems) {
//            try {
//                getJpaController().merge(reglaTrigger);
//            } catch (Exception ex) {
//                addMessage(FacesMessage.SEVERITY_ERROR, "No se pudo persistir el cambio de orden de la regla " + reglaTrigger.getIdTrigger());
//                Logger.getLogger(ReglaTriggerController.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
////        }  
//
//    }

    /**
     * @deprecated @param node
     */
    public void setCatNodeSelected(TreeNode node) {
        Categoria cat = ((Categoria) node.getData());
        System.out.println("setCatNodeSelected: " + cat.getNombre());
        accionTemp.setParametros(cat.getNombre() + " ID[" + cat.getIdCategoria() + "]");
//        condicionTemp.setValor(  cat.getNombre() + " ID[" + cat.getIdCategoria() + "]");
        this.catNodeSelected = node;
    }

    /**
     * @deprecated @return
     */
    public TreeNode getCatNodeSelected() {
        return catNodeSelected;
    }

    public void valueChangeListener(ValueChangeEvent e) {
//        System.out.println(" valueChangeListener() event:" + e);
        Map<String, String> reqParams = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        String idRegla = reqParams.get("idRegla");
        String valor = reqParams.get("valor");
        if ((idRegla == null) || (valor == null)) {
            return;
        }
//        System.out.println("idRegla:" + idRegla);
//        System.out.println("valor:" + valor);
        ReglaTrigger regla = getJpaController().getReglaTriggerFindByIdTrigger(idRegla);
        regla.setReglaActiva(Boolean.valueOf(valor));
        try {
            getJpaController().merge(regla);           
            JsfUtil.addSuccessMessage("Regla " + idRegla + " actualizada");
             recreateModel();

        } catch (Exception ex) {
            Logger.getLogger(ReglaTriggerController.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex, "Error actualizando regla " + idRegla);
        }
    }

    public void handleIdCampoChangeEvent() {
    }

    public void handleAnyChangeEvent() {
    }

//    public void addNewFiltroVista() {
//        Condicion filtro = new Condicion();
//        if (current.getCondicionList() == null || current.getCondicionList().isEmpty()) {
//            current.setCondicionList(new ArrayList<Condicion>());
//        }
//        Random randomGenerator = new Random();
//        int n = randomGenerator.nextInt();
//        if (n > 0) {
//            n = n * (-1);
//        }
//        filtro.setIdCondicion(n);//Ugly patch to solve identifier unknown when new items are added to the datatable.
//        filtro.setIdTrigger(current);
//        System.out.println(filtro);
//        current.getCondicionList().add(filtro);
//
//    }
//    public void removeFiltroFromVista(Condicion filtro) {
//        if (current.getCondicionList() == null || current.getCondicionList().isEmpty()) {
//            JsfUtil.addErrorMessage("No hay criterios en la regla!");
//        }
//        if (current.getCondicionList().contains(filtro)) {
//            current.getCondicionList().remove(filtro);
//        }
//        filtro.setIdTrigger(null);
//    }
    public void crearEmailTemp() {
        emailTemp = new EmailStruct();
    }

    public void setAccionParametroGrupo(Grupo grupo) {
        this.grupoTemp = grupo;
        accionTemp.setParametros(grupo.getIdGrupo());
    }

    public Grupo getAccionParametroGrupo() {
        return grupoTemp;
    }

    public void setAccionParametroArea(Area a) {
        this.areaTemp = a;
        accionTemp.setParametros(a.getIdArea());
    }

    public Area getAccionParametroArea() {
        return areaTemp;
    }

    public void setAccionParametroUsuario(Usuario usuario) {
//        System.out.println("setAccionParametroUsuario " + usuario);
        this.usuarioTemp = usuario;
        accionTemp.setParametros(usuario.getIdUsuario());

    }

    public Usuario getAccionParametroUsuario() {
        return usuarioTemp;
    }

    public void setAccionParametroPrioridad(Prioridad prioridad) {
        this.prioridadTemp = prioridad;
        accionTemp.setParametros(prioridad.getIdPrioridad());

    }

    public Prioridad getAccionParametroPrioridad() {
        return prioridadTemp;
    }


    public boolean esAccionCambioCat() {
        return esAccion(EnumTipoAccion.CAMBIO_CAT);
    }

    public boolean esAccionAsignarArea() {
        return esAccion(EnumTipoAccion.ASIGNAR_A_AREA);
    }

    public boolean esAccionCustom() {
        return esAccion(EnumTipoAccion.CUSTOM);
    }

    public boolean esAccionScheduleAction() {
        return esAccion(EnumTipoAccion.SCHEDULE_ACTION);
    }

    public boolean esAccionAsignarAGrupo() {
        return esAccion(EnumTipoAccion.ASIGNAR_A_GRUPO);
    }

    public boolean esAccionAsignarAUsuario() {
        return esAccion(EnumTipoAccion.ASIGNAR_A_USUARIO);
    }

    public boolean esAccionCambiarPrioridad() {
        return esAccion(EnumTipoAccion.CAMBIAR_PRIORIDAD);
    }

    public boolean esAccionEnviarEmail() {
        return esAccion(EnumTipoAccion.ENVIAR_EMAIL);
    }

    public boolean esAccionRecalcularSLA() {
        return esAccion(EnumTipoAccion.RECALCULAR_SLA);
    }

    public String prepareList() {
        recreateModel();
        return "/script/reglaTrigger/List";
    }

    public String prepareView() {
        if (current == null) {
            return "";
        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "/script/reglaTrigger/View";
    }

    /**
     * This one is not used to filter the data displayed, is used to create a
     * View with filters.
     *
     * @return
     */
    public JPAFilterHelper getFilterHelper2() {
        if (filterHelper2 == null) {
            filterHelper2 = new JPAFilterHelper(vistaCondicionesList, getJpaController()) {
                @Override
                public JPAServiceFacade getJpaService() {
                    return getJpaController();
                }
            };
        }
        return filterHelper2;
    }

    public String prepareEdit(ReglaTrigger item) {
        setSelected(item);
        return prepareEdit();
    }

    public String prepareEdit() {
        if (current == null) {
            return "";
        }
        filterHelper2 = null;
        this.vistaCondicionesList = new Vista(Caso.class);
        for (Condicion condicion : getSelected().getCondicionList()) {
            FiltroVista f = new FiltroVista(condicion.getIdCondicion());
            f.setIdCampo(condicion.getIdCampo());
            f.setIdComparador(condicion.getIdComparador());
            f.setValor(condicion.getValor());
            f.setValor2(condicion.getValor2());
            f.setValoresList(condicion.getValoresList());
            vistaCondicionesList.addFiltroVista(f);
        }

        setAcciones(current.getAccionList());
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "/script/reglaTrigger/Edit";
    }

    public String prepareCreate() {
        current = new ReglaTrigger();
        filterHelper2 = null;
        vistaCondicionesList = new Vista(Caso.class);
        
//        current.setCondicionList(new ArrayList<Condicion>());
        accionTemp = new Accion();
        acciones = new ArrayList<Accion>();
        selectedItemIndex = -1;
        this.current.setOrden(0);
        return "/script/reglaTrigger/Create";
    }

    public String getPropertyAsString(Properties prop) throws IOException {
        StringWriter writer = new StringWriter();
        prop.store(new PrintWriter(writer), "");
        return writer.getBuffer().toString();
    }

    public String addAccionScheduleAction() {

        if (getAcciones() != null) {

            String className = accionTemp.getParametros();
            Properties prop = new Properties();
            prop.put(ScheduleJobAction.ACTION_CLASS, className);
            prop.put(ScheduleJobAction.DATE_FIELD_KEY, this.customFieldKeyDates);
            try {
                accionTemp.setParametros(getPropertyAsString(prop));
            } catch (IOException ex) {
                Logger.getLogger(ReglaTriggerController.class.getName()).log(Level.SEVERE, null, ex);
            }
            accionTemp.setIdTrigger(current);
            Random randomGenerator = new Random();
            int n = randomGenerator.nextInt();
            if (n > 0) {
                n = n * (-1);
            }
            accionTemp.setIdAccion(n);
            getAcciones().add(accionTemp);
        }
//        System.out.println("prepareCreateAccion..." + getAcciones());
        accionTemp = new Accion();
        resetTempVars();
        JsfUtil.addSuccessMessage("Acción agregada");
        executeInClient("addAccionPopup.hide()");
        return null;
    }

    public String prepareCreateAccionEmail() {
        String xml = xstream.toXML(emailTemp);
        accionTemp.setParametros(xml);
        if (getAcciones() != null) {
            accionTemp.setIdTrigger(current);
            Random randomGenerator = new Random();
            int n = randomGenerator.nextInt();
            if (n > 0) {
                n = n * (-1);
            }
            accionTemp.setIdAccion(n);
            getAcciones().add(accionTemp);
        }
        accionTemp = new Accion();
        resetTempVars();
        JsfUtil.addSuccessMessage("Acción agregada");
        executeInClient("mailComposerWidget.hide()");
        
        return null;
    }

    public String prepareCreateAccion() {

        if (esAccionScheduleAction()) {
            return addAccionScheduleAction();
        } else {
            if (getAcciones() != null) {
                accionTemp.setIdTrigger(current);
                Random randomGenerator = new Random();
                int n = randomGenerator.nextInt();
                if (n > 0) {
                    n = n * (-1);
                }
                accionTemp.setIdAccion(n);
                getAcciones().add(accionTemp);
            }
            accionTemp = new Accion();
            resetTempVars();
            JsfUtil.addSuccessMessage("Acción agregada");
            executeInClient("addAccionPopup.hide()");
            
            return null;
        }
    }

    public String create() {
        try {
            if (vistaCondicionesList.getFiltrosVistaList() == null || vistaCondicionesList.getFiltrosVistaList().isEmpty()) {
                addErrorMessage("Favor agregar al menos una condición.");
                return null;
            }

            if (acciones == null || acciones.isEmpty()) {
                addErrorMessage("Favor agregar al menos una acción.");
                return null;
            }

            current.setCondicionList(new LinkedList<Condicion>());

            for (FiltroVista filtroVista : vistaCondicionesList.getFiltrosVistaList()) {
                Condicion c = new Condicion(null);
                c.setIdCampo(filtroVista.getIdCampo());
                c.setIdComparador(filtroVista.getIdComparador());
                c.setValor(filtroVista.getValor());
                c.setValor2(filtroVista.getValor2());
                c.setValoresList(filtroVista.getValoresList());
                c.setIdTrigger(current);
                current.getCondicionList().add(c);
            }
//            for (Condicion c : current.getCondicionList()) {
//                c.setIdCondicion(null);
//            }

            for (Accion accion : acciones) {
                accion.setIdAccion(null);
            }

            current.setAccionList(acciones);
            getJpaController().persist(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ReglaTriggerCreated"));
            recreateModel();
            return prepareList();
        } catch (Exception e) {
            e.printStackTrace();
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String update() {
        try {
            if (vistaCondicionesList.getFiltrosVistaList() == null || vistaCondicionesList.getFiltrosVistaList().isEmpty()) {
                addErrorMessage("Favor agregar al menos una condición.");
                return null;
            }

            if (acciones == null || acciones.isEmpty()) {
                addErrorMessage("Favor agregar al menos una acción.");
                return null;
            }

            current.setCondicionList(new LinkedList<Condicion>());

            for (FiltroVista filtroVista : vistaCondicionesList.getFiltrosVistaList()) {
                Condicion c = new Condicion(filtroVista.getIdFiltro());
                c.setIdCampo(filtroVista.getIdCampo());
                c.setIdComparador(filtroVista.getIdComparador());
                c.setValor(filtroVista.getValor());
                c.setValor2(filtroVista.getValor2());
                c.setValoresList(filtroVista.getValoresList());
                c.setIdTrigger(current);
                current.getCondicionList().add(c);
            }

            for (Condicion condicion : current.getCondicionList()) {
                if (condicion.getIdCondicion() < 0) {
                    condicion.setIdCondicion(null);
                }
            }

            for (Accion accion : acciones) {
                if (accion.getIdAccion() < 0) {
                    accion.setIdAccion(null);
                }
            }
            current.setAccionList(acciones);
            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ReglaTriggerUpdated"));
            recreateModel();
            return prepareList();
        } catch (Exception e) {
            e.printStackTrace();
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String update(ReglaTrigger regla) {
        try {
//            System.out.println(regla + " regla estado:" + regla.getReglaActiva());
            getJpaController().merge(regla);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ReglaTriggerUpdated"));
            return prepareEdit();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        if (current == null) {
            return "";
        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "/script/reglaTrigger/List";
    }

    public String destroySelected() {
        if (current == null) {
            return "";
        }
        performDestroy();
//        try {
//            selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
//        } finally {
//            recreateModel();
//        }
        recreateModel();
        return "/script/reglaTrigger/List";
    }

//    public String destroyAndView() {
//        performDestroy();
//        recreateModel();
//        updateCurrentItem();
//        if (selectedItemIndex >= 0) {
//            return "/script/reglaTrigger/View";
//        } else {
//            // all items were removed - go back to list
//            recreateModel();
//            return "/script/reglaTrigger/List";
//        }
//    }

    private void performDestroy() {
        try {
            getJpaController().remove(getEntityClass(),current.getIdTrigger());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ReglaTriggerDeleted"));
        } catch (Exception e) {
            e.printStackTrace();
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

//    private void updateCurrentItem() {
//        int count = getJpaController().count(ReglaTrigger.class).intValue();
//        if (selectedItemIndex >= count) {
//            // selected index cannot be bigger than number of items:
//            selectedItemIndex = count - 1;
//            // go to previous page if last page disappeared:
//            if (pagination.getPageFirstItem() >= count) {
//                pagination.previousPage();
//            }
//        }
//        if (selectedItemIndex >= 0) {
//            current = (ReglaTrigger) getJpaController().queryByRange(ReglaTrigger.class, 1, selectedItemIndex).get(0);
//        }
//    }


    /**
     * @return the acciones
     */
    public List<Accion> getAcciones() {
        return acciones;
    }

    /**
     * @param acciones the acciones to set
     */
    public void setAcciones(List<Accion> acciones) {
        this.acciones = acciones;
    }

    /**
     * @return the accionTemp
     */
    public Accion getAccionTemp() {
        return accionTemp;
    }

    /**
     * @param accionTemp the accionTemp to set
     */
    public void setAccionTemp(Accion accionTemp) {
        this.accionTemp = accionTemp;
    }

    public void resetTempVars() {
        System.out.println("resetTempVars");
        usuarioTemp = null;
        prioridadTemp = null;
        grupoTemp = null;
        emailTemp = null;
        accionTemp.setParametros(null);
        catNodeSelected = null;
    }

    /**
     * @return the emailTemp
     */
    public EmailStruct getEmailTemp() {
        return emailTemp;
    }

    /**
     * @param emailTemp the emailTemp to set
     */
    public void setEmailTemp(EmailStruct emailTemp) {
        this.emailTemp = emailTemp;
    }

    private boolean esAccion(EnumTipoAccion enumAccion) {
        if ((accionTemp == null) || (accionTemp.getIdTipoAccion() == null)) {
            return false;
        }
        return accionTemp.getIdTipoAccion().equals(enumAccion.getTipoAccion());
    }

    @Override
    public OrderBy getDefaultOrderBy() {
        return new OrderBy("orden");
    }

    @Override
    public Class getDataModelImplementationClass() {
        return ReglaTriggerDataModel.class;
    }

    /**
     * @return the customFieldKeyDates
     */
    public String getCustomFieldKeyDates() {
        return customFieldKeyDates;
    }

    /**
     * @param customFieldKeyDates the customFieldKeyDates to set
     */
    public void setCustomFieldKeyDates(String customFieldKeyDates) {
        this.customFieldKeyDates = customFieldKeyDates;
    }

    @FacesConverter(forClass = ReglaTrigger.class, value = "ReglaTriggerControllerConverter")
    public static class ReglaTriggerControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ReglaTriggerController controller = (ReglaTriggerController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "reglaTriggerController");
            return controller.getJpaController().getReglaTriggerFindByIdTrigger(getKey(value));
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
            if (object instanceof ReglaTrigger) {
                ReglaTrigger o = (ReglaTrigger) object;
                return getStringKey(o.getIdTrigger());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + ReglaTrigger.class.getName());
            }
        }
    }
}

class ReglaTriggerDataModel extends ListDataModel<ReglaTrigger> implements SelectableDataModel<ReglaTrigger> {

    public ReglaTriggerDataModel() {
        //nothing
    }

    public ReglaTriggerDataModel(List<ReglaTrigger> data) {
        super(data);
    }

    @Override
    public ReglaTrigger getRowData(String rowKey) {
        List<ReglaTrigger> listOfReglaTrigger = (List<ReglaTrigger>) getWrappedData();

        for (ReglaTrigger obj : listOfReglaTrigger) {
            if (obj.getIdTrigger().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(ReglaTrigger classname) {
        return classname.getIdTrigger();
    }
}