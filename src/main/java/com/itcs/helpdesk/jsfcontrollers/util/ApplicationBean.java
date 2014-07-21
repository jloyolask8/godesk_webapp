/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.jsfcontrollers.util;

import com.itcs.helpdesk.jsfcontrollers.AbstractManagedBean;
import com.itcs.helpdesk.persistence.entities.Archivo;
import com.itcs.helpdesk.persistence.entities.Area;
import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.Etiqueta;
import com.itcs.helpdesk.persistence.entities.FieldType;
import com.itcs.helpdesk.persistence.entities.FiltroVista;
import com.itcs.helpdesk.persistence.entities.Prioridad;
import com.itcs.helpdesk.persistence.entities.ReglaTrigger;
import com.itcs.helpdesk.persistence.entities.Resource;
import com.itcs.helpdesk.persistence.entities.TipoAlerta;
import com.itcs.helpdesk.persistence.entities.TipoCaso;
import com.itcs.helpdesk.persistence.entities.Vista;
import com.itcs.helpdesk.persistence.entityenums.EnumAreas;
import com.itcs.helpdesk.persistence.entityenums.EnumEstadoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAccion;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAlerta;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoComparacion;
import com.itcs.helpdesk.persistence.jpa.custom.CasoJPACustomController;
import com.itcs.helpdesk.rules.Action;
import com.itcs.helpdesk.rules.actionsimpl.SendCaseByEmailAction;
import com.itcs.helpdesk.util.ApplicationConfig;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.model.SelectItem;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;

/**
 *
 * @author jonathan
 */
@ManagedBean
@ApplicationScoped
public class ApplicationBean extends AbstractManagedBean<Object> implements Serializable {

//    private String defaultContactEmail = null;
    //  Map<String, String> loggedInUsers = new HashMap<String, String>();
    private Map<String, String> channels = new HashMap<String, String>();
    //--pre created Vistas
    private final transient Map<Integer, Vista> predefinedVistas = new HashMap<Integer, Vista>();
    private Vista vistaRevisarActualizacion;
    private final transient Map<String, Action> predefinedActions = new HashMap<String, Action>();
    private String ckEditorToolbar = "[{ name: 'document', items : ['Preview', 'SpellChecker', 'Scayt', 'Link', 'Unlink', 'Iframe', 'Image','Table','HorizontalRule','NumberedList','BulletedList'] },"
            + "{ name: 'style', items : ['Bold','Italic','Underline','TextColor','BGColor', '-','RemoveFormat','Blockquote'] },"
            + "{ name: 'style', items : ['Styles','Format','Maximize']}]";

    
     public String getSubDomain() {
        return getHostSubDomain(JsfUtil.getRequest().getRequestURL().toString());
    }
     
    @PostConstruct
    private void init() {
        for (EnumTipoAlerta enumTipoAlerta : EnumTipoAlerta.values()) {
            predefinedVistas.put(enumTipoAlerta.getTipoAlerta().getIdalerta(), createVistaPorAlerta(enumTipoAlerta.getTipoAlerta()));
        }
        vistaRevisarActualizacion = createVistaRevisarActualizacion();

        SendCaseByEmailAction a = new SendCaseByEmailAction();
        predefinedActions.put(EnumTipoAccion.ENVIAR_CASO_POR_EMAIL.getTipoAccion().getIdTipoAccion(), a);
    }

    /**
     * @return the vistaRevisarActualizacion
     */
    public Vista getVistaRevisarActualizacion() {
        return vistaRevisarActualizacion;
    }

    public Vista getVistaPorAlerta(TipoAlerta alerta) {
        return predefinedVistas.get(alerta.getIdalerta());
    }

    private Vista createVistaRevisarActualizacion() {
        Vista vista1 = new Vista(Caso.class);
        vista1.setIdUsuarioCreadaPor(((UserSessionBean) JsfUtil.getManagedBean("UserSessionBean")).getCurrent());
        vista1.setNombre("Casos recientemente Actualizados");

        FiltroVista filtroOwner = new FiltroVista();
        filtroOwner.setIdFiltro(-1);
        filtroOwner.setIdCampo("owner");
        filtroOwner.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
        filtroOwner.setValor(CasoJPACustomController.PLACE_HOLDER_CURRENT_USER);
        filtroOwner.setIdVista(vista1);

        vista1.getFiltrosVistaList().add(filtroOwner);

        FiltroVista reviewUpdate = new FiltroVista();
        reviewUpdate.setIdFiltro(-2);
        reviewUpdate.setIdCampo("revisarActualizacion");
        reviewUpdate.setVisibleToAgents(false);
        reviewUpdate.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
        reviewUpdate.setValor(Boolean.TRUE.toString());
        reviewUpdate.setIdVista(vista1);
        vista1.getFiltrosVistaList().add(reviewUpdate);
        return vista1;
    }

    private Vista createVistaPorAlerta(TipoAlerta alerta) {
//        //System.out.println("filtraPorAlerta");
        Vista vista1 = new Vista(Caso.class);
        vista1.setIdUsuarioCreadaPor(((UserSessionBean) JsfUtil.getManagedBean("UserSessionBean")).getCurrent());
        vista1.setNombre("Casos con Alerta " + alerta.getNombre());

        FiltroVista filtroOwner = new FiltroVista();
        filtroOwner.setIdFiltro(-1);
        filtroOwner.setIdCampo("owner");
        filtroOwner.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
        filtroOwner.setValor(CasoJPACustomController.PLACE_HOLDER_CURRENT_USER);
        filtroOwner.setIdVista(vista1);

        vista1.getFiltrosVistaList().add(filtroOwner);

        FiltroVista filtroEstado = new FiltroVista();
        filtroOwner.setIdFiltro(-2);
        filtroEstado.setIdCampo("idEstado");
        filtroEstado.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
        filtroEstado.setValor(EnumEstadoCaso.ABIERTO.getEstado().getIdEstado());
        filtroEstado.setIdVista(vista1);

        vista1.getFiltrosVistaList().add(filtroEstado);

        FiltroVista filtroAlerta = new FiltroVista();
        filtroOwner.setIdFiltro(-3);
        filtroAlerta.setIdCampo("estadoAlerta");
        filtroAlerta.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
        filtroAlerta.setValor(alerta.getIdalerta().toString());
        filtroAlerta.setIdVista(vista1);

        vista1.getFiltrosVistaList().add(filtroAlerta);

        return vista1;
    }

    /**
     * Creates a new instance of ApplicationBean
     */
    public ApplicationBean() {
        super(Object.class);
    }

    public void addChannel(String user, String channel) {
        channels.put(user, channel);
    }

    public String getChannel(String user) {
        return channels.get(user);
    }

    public void sendFacesMessageNotification(String idUsuario, String m) {

        PushContext pushContext = PushContextFactory.getDefault().getPushContext();
        final String channel = getChannel(idUsuario);

//        System.out.println("SENDING NOTIFICATION TO " + channel);
        if (channel != null) {
//            System.out.println("SENT NOTIFICATION TO " + channel);
            pushContext.push(channel, new FacesMessage(m));
        } else {
            System.out.println("CHANNEL NOT OPPENED, COULD NOT SEND NOTIFICATION TO " + idUsuario);
        }

    }

    public Date getNow() {
        return new java.util.Date();
    }

    public SelectItem[] getEtiquetaItemsAvailableSelect() {
        return JsfUtil.getSelectItemsStrings(getJpaController().findAll(Etiqueta.class), false);
    }

    public List<Etiqueta> findEtiquetasByPattern(String pattern) {
        List<Etiqueta> results = new ArrayList<Etiqueta>();
        if (pattern != null) {
            results.add(new Etiqueta(pattern.trim()));
            results.addAll(getJpaController().findEtiquetasLike(pattern.trim(), getUserSessionBean().getCurrent().getIdUsuario()));
        }
        //return ((List<Etiqueta>) getJpaController().findEtiquetasLike(pattern));
        return results;
    }

    public List<Etiqueta> getEtiquetasAll() {
        return (List<Etiqueta>) getJpaController().findAll(Etiqueta.class);
    }
    
    public List<Resource> getResourceAll() {
        return (List<Resource>) getJpaController().findAll(Resource.class);
    }

    public List<ReglaTrigger> getReglasAll() {
        return (List<ReglaTrigger>) getJpaController().findAll(ReglaTrigger.class);
    }

    public List<FieldType> getAllFieldTypes() {
        return getJpaController().getCustomFieldTypes();
    }

    @Override
    public PaginationHelper getPagination() {
        throw new UnsupportedOperationException("This operation is Not supported!.");
    }

    /**
     * @return the appPageTitle
     */
    public String getHelpdeskTitle() {
        if (getUserSessionBean() != null && !StringUtils.isEmpty(getUserSessionBean().getTenantId())) {
            return (ApplicationConfig.getInstance(getUserSessionBean().getTenantId()) != null) ? ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).getHelpdeskTitle():"";

        }
        return "<title>";
    }

    public String getCompanyName() {
        if (getUserSessionBean() != null && !StringUtils.isEmpty(getUserSessionBean().getTenantId())) {
            return ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).getCompanyName();

        }
        return "<companyName>";
    }

    public String getCompanyDefaultContactEmail() {
//        if (defaultContactEmail == null) {
        String defaultContactEmail = "<email>";
        String idDefaultArea = EnumAreas.DEFAULT_AREA.getArea().getIdArea();
        System.out.println("find(" + idDefaultArea + ")");
        Area a = getJpaController().find(Area.class, idDefaultArea);
        if (a != null && !StringUtils.isEmpty(a.getMailInboundUser())) {
            defaultContactEmail = a.getMailInboundUser();
        }
//        }

        return defaultContactEmail;

    }

    public String getProductDescription() {
        if (getUserSessionBean() != null && !StringUtils.isEmpty(getUserSessionBean().getTenantId())) {
            return ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).getProductDescription();
        }
        return "<ProductDescription>";
    }

    public String getProductComponentDescription() {
        if (getUserSessionBean() != null && !StringUtils.isEmpty(getUserSessionBean().getTenantId())) {
            return ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).getProductComponentDescription();
        }
        return "<ProductComponentDescription>";
    }

    public String getProductSubComponentDescription() {
        if (getUserSessionBean() != null && !StringUtils.isEmpty(getUserSessionBean().getTenantId())) {
            return ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).getProductSubComponentDescription();
        }
        return "<ProductSubComponentDescription>";
    }

    public List<TipoCaso> getTipoCasoAvailableList() {
        return (List<TipoCaso>) getJpaController().findAll(TipoCaso.class);
    }

    public List<Prioridad> getPrioridadItemsAvailableList() {
        return (List<Prioridad>) getJpaController().findAll(Prioridad.class);
    }

    public boolean isShowCompanyLogo() {
        if (getUserSessionBean() != null && !StringUtils.isEmpty(getUserSessionBean().getTenantId())) {
            return ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).isShowCompanyLogo();
        }
        return false;
    }

    public StreamedContent getLogo() {

        if (getUserSessionBean() != null && !StringUtils.isEmpty(getUserSessionBean().getTenantId())) {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context.getCurrentPhaseId() != PhaseId.RENDER_RESPONSE) {
                // So, we're rendering the view. Return a stub StreamedContent so that it will generate right URL.
                Archivo archivo = getJpaController().find(Archivo.class, 0L);
                if (archivo != null) {
                    return new DefaultStreamedContent(
                            new ByteArrayInputStream(archivo.getArchivo()), archivo.getContentType(), archivo.getFileName());
                }
            }
        }

        return new DefaultStreamedContent();

    }

    @Override
    public Class getDataModelImplementationClass() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @return the defaultContactEmail
     */
//    public String getDefaultContactEmail() {
//        return defaultContactEmail;
//    }
    /**
     * @return the predefinedActions
     */
    public Set<String> getPredefinedActionsAsString() {
        return predefinedActions.keySet();
    }

    public Map<String, Action> getPredefinedActions() {
        return predefinedActions;
    }

    /**
     * @return the ckEditorToolbar
     */
    public String getCkEditorToolbar() {
        return ckEditorToolbar;
    }

    /**
     * @param ckEditorToolbar the ckEditorToolbar to set
     */
    public void setCkEditorToolbar(String ckEditorToolbar) {
        this.ckEditorToolbar = ckEditorToolbar;
    }
}
