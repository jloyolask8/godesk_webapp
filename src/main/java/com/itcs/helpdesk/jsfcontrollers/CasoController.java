package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.commons.email.EmailClient;
import com.itcs.helpdesk.jsfcontrollers.util.ApplicationBean;
import com.itcs.helpdesk.jsfcontrollers.util.FiltroAcceso;
import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.jsfcontrollers.util.UserSessionBean;
import com.itcs.helpdesk.persistence.entities.Archivo;
import com.itcs.helpdesk.persistence.entities.Attachment;
import com.itcs.helpdesk.persistence.entities.AuditLog;
import com.itcs.helpdesk.persistence.entities.BlackListEmail;
import com.itcs.helpdesk.persistence.entities.Canal;
import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.CasoCustomField;
import com.itcs.helpdesk.persistence.entities.Categoria;
import com.itcs.helpdesk.persistence.entities.Cliente;
import com.itcs.helpdesk.persistence.entities.Clipping;
import com.itcs.helpdesk.persistence.entities.CustomField;
import com.itcs.helpdesk.persistence.entities.EmailCliente;
import com.itcs.helpdesk.persistence.entities.EstadoCaso;
import com.itcs.helpdesk.persistence.entities.Etiqueta;
import com.itcs.helpdesk.persistence.entities.FiltroVista;
import com.itcs.helpdesk.persistence.entities.ModeloProducto;
import com.itcs.helpdesk.persistence.entities.Nota;
import com.itcs.helpdesk.persistence.entities.Prioridad;
import com.itcs.helpdesk.persistence.entities.ProductoContratado;
import com.itcs.helpdesk.persistence.entities.ReglaTrigger;
import com.itcs.helpdesk.persistence.entities.SubEstadoCaso;
import com.itcs.helpdesk.persistence.entities.TipoAlerta;
import com.itcs.helpdesk.persistence.entities.TipoNota;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.persistence.entities.Vista;
import com.itcs.helpdesk.persistence.entityenums.EnumCanal;
import com.itcs.helpdesk.persistence.entityenums.EnumEstadoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumFunciones;
import com.itcs.helpdesk.persistence.entityenums.EnumPrioridad;
import com.itcs.helpdesk.persistence.entityenums.EnumSubEstadoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAlerta;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoComparacion;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoNota;
import com.itcs.helpdesk.persistence.jpa.AbstractJPAController;
import com.itcs.helpdesk.persistence.jpa.exceptions.IllegalOrphanException;
import com.itcs.helpdesk.persistence.jpa.exceptions.NonexistentEntityException;
import com.itcs.helpdesk.persistence.jpa.exceptions.PreexistingEntityException;
import com.itcs.helpdesk.persistence.jpa.exceptions.RollbackFailureException;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import com.itcs.helpdesk.persistence.utils.OrderBy;
import com.itcs.helpdesk.rules.Action;
import com.itcs.helpdesk.rules.ActionExecutionException;
import com.itcs.helpdesk.util.ApplicationConfig;
import com.itcs.helpdesk.util.ClippingsPlaceHolders;
import com.itcs.helpdesk.quartz.HelpDeskScheluder;
import com.itcs.helpdesk.util.HtmlUtils;
import com.itcs.helpdesk.util.Log;
import com.itcs.helpdesk.util.MailClientFactory;
import com.itcs.helpdesk.util.MailNotifier;
import com.itcs.helpdesk.util.ManagerCasos;
import com.itcs.helpdesk.util.PrettyDate;
import com.itcs.helpdesk.util.RulesEngine;
import com.itcs.helpdesk.util.UtilesRut;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.resource.NotSupportedException;
import javax.servlet.http.HttpServletRequest;
import jxl.Workbook;
import jxl.format.UnderlineStyle;
import jxl.write.DateFormat;
import jxl.write.DateTime;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.FlowEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.SelectableDataModel;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.UploadedFile;
import org.primefaces.model.tagcloud.DefaultTagCloudItem;
import org.primefaces.model.tagcloud.DefaultTagCloudModel;
import org.primefaces.model.tagcloud.TagCloudItem;
import org.primefaces.model.tagcloud.TagCloudModel;
import org.quartz.SchedulerException;

@ManagedBean(name = "casoController")
@SessionScoped
public class CasoController extends AbstractManagedBean<Caso> implements Serializable {

    private static final Locale LOCALE_ES_CL = new Locale("es", "CL");
    private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEE, dd 'de' MMMM 'de' yyyy HH:mm", LOCALE_ES_CL);
//    @Inject
//    private Event<BackendEvent> backendEvent;
    @ManagedProperty(value = "#{applicationBean}")
    private ApplicationBean applicationBean;
    @ManagedProperty(value = "#{filtroAcceso}")
    private FiltroAcceso filtroAcceso;
    @ManagedProperty(value = "#{vistaController}")
    private VistaController vistaController;
    @ManagedProperty(value = "#{UserSessionBean}")
    protected UserSessionBean userSessionBean;

    protected transient JPAServiceFacade jpaController = null;
//       @ManagedProperty(value = "#{applicationBean}")
//    private ApplicationBean applicationBean;
//    private Caso current;
//    private Caso[] selectedItems = new Caso[0];
    private List<Etiqueta> selectedEtiquetas = new ArrayList<Etiqueta>();
    private transient HashMap<String, BlackListEmail> blackListMap;
//    private transient DataModel items = null;
//    private transient PaginationHelper pagination;
    private int selectedItemIndex;
//    private int activeIndexMenuAccordionPanel;
    private int activeIndexWestPanel;
    private int activeIndexWestPanelForms;
    private int activeIndexdescOrComment;
    //Notas
    private String textoNota = "";
    private boolean textoNotaVisibilidadPublica = false;
//    private TipoNota tipoNota;
//    private float laborTime = 0;
    private List<String> tipoNotas;
    //Clippings
    private Clipping selectedClipping;
    private boolean adjuntarArchivosARespuesta = false;
    /*
     * Objetos para filtro
     */
    private transient TreeNode categoria;
    private Long idCaso;
    /*
     * Objetos para attachments
     */
    private transient UploadedFile uploadFile;
    private String idFileDelete = "";
    private List<String> selectedAttachmensForMail;
//    private List<Attachment> attachmentWOContentId;
    private int cantidadDeNotas;
//    private int cantidadAttachmentsEmbedded = 0;
    private int cantidadDeRespuestasACliente;
    private int cantidadDeRespuestasDelCliente;
    private static transient ResourceBundle resourceBundle = ResourceBundle.getBundle("Bundle");
    private List<Nota> listaActividadesOrdenada = Collections.EMPTY_LIST;
    private boolean incluirHistoria;
//    private Integer progresoEnvioRespuesta;
    private Categoria categorySelected;
    private ReglaTrigger reglaTriggerSelected;
    private String emailCliente_wizard;
    private String rutCliente_wizard;
    private boolean emailCliente_wizard_existeEmail = false;
    private boolean emailCliente_wizard_existeCliente = false;
    private boolean emailCliente_wizard_updateCliente = false;
    private String htmlToView = null;
//    private boolean filtrarPorCategorias;
//    private boolean filtrarPorVista = false;
//    private Vista vista;
    private Integer visibilityOption = 1;
//    private transient JPAFilterHelper filterHelper;
    //mobile
    private String swatch = "b";
    private Nota selectedNota;
    // transfer
    private Integer tipoTransferOption = 1;
    private Usuario usuarioSeleccionadoTransfer;
    private EmailCliente emailClienteSeleccionadoTransfer;
    private Long idFileRemove;
    private Integer justCreadedNotaId;
    private Integer selectedViewId;//Vista seleccionada
    //customer
    private int stepNewCasoIndex;
    //actions macros
    private String accionToRunSelected;
    private String accionToRunParametros;
    //reply-mode
    private boolean replyMode = false;
    private boolean filterViewToggle = false;

    public CasoController() {
        super(Caso.class);
    }

    @Override
    public Class getDataModelImplementationClass() {
        return CasoDataModel.class;
    }

    @Override
    public OrderBy getDefaultOrderBy() {
        return new OrderBy("fechaCreacion", OrderBy.OrderType.DESC);
    }

    @Override
    public Usuario getDefaultUserWho() {
        return userSessionBean.getCurrent();
    }

    public void enableReplyMode() {
        this.replyMode = true;
    }

    public void disableReplyMode() {
        this.replyMode = false;
        this.selectedClipping = null;
        this.textoNota = null;
        this.adjuntarArchivosARespuesta = false;
    }

//    @PostConstruct
    public String inbox() {
        idCaso = null;
        pagination = null;
        items = null;
        prepareCasoFilterForInbox();
        return "inbox";
    }

    /**
     * @param filtroAcceso the filtroAcceso to set
     */
    public void setFiltroAcceso(FiltroAcceso filtroAcceso) {
        this.filtroAcceso = filtroAcceso;
    }

    public void handleTagSelect() {
    }

    public void handleProductChange() {
    }

    public void onIndexWestPanelFormsTabChanged(TabChangeEvent event) {
        //THIS CUERO PIC PULGA ES A RAIZ DE UN BUG DE PRIMEFACES, ME ENVIA EL ACTIVE INDEX SIEMPRE EN CER0.
        final String idtab = event.getTab().getId();
        if (idtab.contains("tbaInfo")) {
            setActiveIndexWestPanelForms(0);
        } else if (idtab.contains("tbaCaso")) {
            setActiveIndexWestPanelForms(1);
        } else if (idtab.contains("tbaCliente")) {
            setActiveIndexWestPanelForms(2);
        } else {
            setActiveIndexWestPanelForms(0);

        }
    }

    public void onChangeActiveIndexdescOrComment(TabChangeEvent event) {
        //THIS CUERO PIC PULGA ES A RAIZ DE UN BUG DE PRIMEFACES, ME ENVIA EL ACTIVE INDEX SIEMPRE EN CER0.
        final String idtab = event.getTab().getId();
        if (idtab.contains("tab-actividades")) {
            setActiveIndexdescOrComment(0);
        } else if (idtab.contains("tabAgendarEvento")) {
            setActiveIndexdescOrComment(1);
        } else if (idtab.contains("tab-timeline")) {
            setActiveIndexdescOrComment(2);
        } else {
            setActiveIndexdescOrComment(0);
        }
    }

    public List<AuditLog> getAuditLogsCurrentCase() {
        Vista vista1 = new Vista(AuditLog.class);
        vista1.setIdUsuarioCreadaPor(userSessionBean.getCurrent());
        vista1.setNombre("Audit Logs");

        FiltroVista f1 = new FiltroVista();
        f1.setIdCampo("idCaso");
        f1.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
        f1.setValor(current.getIdCaso().toString());
        f1.setIdVista(vista1);

        vista1.getFiltrosVistaList().add(f1);

        try {
            return (List<AuditLog>) getJpaController()
                    .findAllEntities(AuditLog.class, vista1, new OrderBy("fecha", OrderBy.OrderType.DESC), userSessionBean.getCurrent());
        } catch (NotSupportedException ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "NotSupportedException", ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "ClassNotFoundException", ex);
        }
        return Collections.EMPTY_LIST;
    }

    public TagCloudModel getCasoTagModel() {
        TagCloudModel model = new DefaultTagCloudModel();
        if (current.getEtiquetaList() != null) {
            for (Etiqueta etiqueta : current.getEtiquetaList()) {
                model.addTag(new DefaultTagCloudItem(etiqueta.getTagId(), 1));
            }
        }
        return model;
    }

    public void tagItemSelectEvent(SelectEvent event) {
        Object item = event.getObject();
        current.setFechaModif(Calendar.getInstance().getTime());
        try {
            if (current.getEtiquetaList() != null) {
                for (Etiqueta etiqueta : current.getEtiquetaList()) {
                    etiqueta.setOwner(userSessionBean.getCurrent());
                    if (etiqueta.getCasoList() == null) {
                        etiqueta.setCasoList(new LinkedList<Caso>());
                    }
                    etiqueta.getCasoList().add(current);
                }
            }
            getJpaController().mergeCaso(current, getManagerCasos().createLogReg(current, "Etiquetas", current.getEtiquetaList().toString(), ""));
            addInfoMessage("Etiqueta Agregada OK!");
        } catch (Exception ex) {
            addInfoMessage("No se pudo Agregar la etiqueta" + item);
            Log.createLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void tagItemUnselectEvent(UnselectEvent event) {
        Object item = event.getObject();
        current.setFechaModif(Calendar.getInstance().getTime());
        try {
            getJpaController().mergeCaso(current, getManagerCasos().createLogReg(current, "Etiquetas", current.getEtiquetaList().toString(), ""));
//            addInfoMessage("Etiqueta Removida OK!");
        } catch (Exception ex) {
            addInfoMessage("No se pudo Remover la etiqueta" + item);
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public boolean isCasoTipoPreventa() {
        try {
            if (current.getTipoCaso().equals(EnumTipoCaso.PREVENTA.getTipoCaso())) {
                return true;
            }
        } catch (Exception e) {
            //ignore
        }

        return false;

    }

    public List<String> getSelectedBlackList() {
        try {
            if (getSelectedItems() != null && getSelectedItems().size() > 0) {
                blackListMap = new HashMap<String, BlackListEmail>();
                for (Caso caso : getSelectedItems()) {
                    blackListMap.put(caso.getEmailCliente().getEmailCliente(), new BlackListEmail(caso.getEmailCliente().getEmailCliente()));
                }
                return new ArrayList<String>(blackListMap.keySet());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void saveBlackList() {

        for (BlackListEmail blackListEmail : blackListMap.values()) {
            BlackListEmail persistentBlackListEmail = getJpaController().find(BlackListEmail.class, blackListEmail.getEmailAddress());
            if (persistentBlackListEmail == null) {
                try {
                    getJpaController().persist(blackListEmail);
                    addInfoMessage(blackListEmail.getEmailAddress() + " guardado en lista negra OK.");
                } catch (Exception ex) {
                    Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
                    addErrorMessage(blackListEmail.getEmailAddress() + " Error:" + ex.getLocalizedMessage());
                }
            }
        }

        recreateModel();

    }

    public void changePriority(Caso caso, boolean esPrioritario) {
        try {
            caso.setEsPrioritario(!esPrioritario);
            getJpaController().merge(caso);
        } catch (Exception ex) {
            caso.setEsPrioritario(esPrioritario);
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void removeCategory() {
        try {
            current.setIdCategoria(null);
            getJpaController().merge(current);
        } catch (Exception ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "error at removeCategory", ex);
        }
    }

    public void chooseProductoModelo() {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Modelo Producto Selected", "Model:  Product:");
        RequestContext.getCurrentInstance().showMessageInDialog(message);
        RequestContext.getCurrentInstance().openDialog("selectProductoModelo");
    }

    public void onProductoModeloChosen() {
        //ModeloProducto modeloProducto = (ModeloProducto) event.getObject();
        current.setIdComponente(current.getIdModelo().getIdComponente());
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Modelo Producto Selected", "Model:" + current.getIdModelo() + " Product:" + current.getIdProducto().getNombre()
                + "component:" + current.getIdComponente().getNombre());
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void selectProductoModeloFromDialog(ModeloProducto modeloProducto) {
        // RequestContext.getCurrentInstance().closeDialog(modeloProducto);
        current.setIdModelo(modeloProducto);
    }

    public void handleEmailSelect(SelectEvent event) {

        try {
            EmailCliente emailCliente = getJpaController().find(EmailCliente.class, getEmailCliente_wizard());

//        //System.out.println("emailCliente_wizard:" + emailCliente_wizard);
//        //System.out.println("event.getObject().toString():" + event.getObject().toString());
            if (emailCliente != null) {

                current.setEmailCliente(emailCliente);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Email cliente registrado.", null);
                FacesContext.getCurrentInstance().addMessage("form:emailCliente", message);
                setEmailCliente_wizard_existeEmail(true);
                if (emailCliente.getCliente() != null) {
                    rutCliente_wizard = emailCliente.getCliente().getRut();
                    setEmailCliente_wizard_existeCliente(true);
                } else {
                    setEmailCliente_wizard_existeCliente(false);
                    Cliente cliente = new Cliente();
                    //cliente.setRut(rutCliente_wizard);
                    emailCliente.setCliente(cliente);
                }
            } else {

                setEmailCliente_wizard_existeEmail(false);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Email " + emailCliente_wizard + " no registrado, favor ingresar los datos del cliente.", null);
                FacesContext.getCurrentInstance().addMessage("form:emailCliente", message);

                emailCliente = new EmailCliente(getEmailCliente_wizard());
                if (rutCliente_wizard != null) {
                    //user already entered rut, so its posible there is a cliente selected.
                    //if i change the email, to an email that is not in database, it means i want to add a new email to an existent client.
                    Cliente existentClient = getJpaController().findClienteByRut(rutCliente_wizard);
                    if (existentClient == null) {
                        //not exists
                        Cliente cliente = new Cliente();
                        cliente.setRut(rutCliente_wizard);
                        emailCliente.setCliente(cliente);
                        setEmailCliente_wizard_existeCliente(false);
                    } else {
                        //yes, it exists...
                        emailCliente.setCliente(existentClient);
                        setEmailCliente_wizard_existeCliente(true);
                    }
                } else {
                    setEmailCliente_wizard_existeCliente(false);
                    emailCliente.setCliente(new Cliente());
                }

                current.setEmailCliente(emailCliente);
            }
        } catch (Exception ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "error at handleEmailSelect", ex);
            addErrorMessage(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }

    }

    public void formateaRutFiltro2() {
        //System.out.println("formateaRutFiltro2()");
        try {
//            final String rutInput = getSelected().getEmailCliente().getCliente().getRut();

            if (rutCliente_wizard != null && !StringUtils.isEmpty(rutCliente_wizard)) {
                rutCliente_wizard = UtilesRut.formatear(rutCliente_wizard);

                if (!emailCliente_wizard_existeCliente) {
                    Cliente c = getJpaController().findClienteByRut(rutCliente_wizard);
                    if (c != null) {//this client exists
                        rutCliente_wizard = c.getRut();
                        setEmailCliente_wizard_existeCliente(true);

                        if (c.getEmailClienteList() != null && !c.getEmailClienteList().isEmpty()) {
                            EmailCliente emailCliente = c.getEmailClienteList().get(0);
                            current.setEmailCliente(emailCliente);
                            emailCliente_wizard = emailCliente.getEmailCliente();
                            setEmailCliente_wizard_existeEmail(true);
                        } else {
                            emailCliente_wizard = null;
                            setEmailCliente_wizard_existeEmail(false);
                        }
                    } else {

                        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "El cliente con rut " + rutCliente_wizard + " no está registrado, favor ingresar.", null);
                        FacesContext.getCurrentInstance().addMessage("form:rut", message);
                        Cliente cliente = new Cliente();
                        cliente.setRut(rutCliente_wizard);

                        if (current.getEmailCliente() != null) {
                            current.getEmailCliente().setCliente(cliente);
                        } else {
                            if (getEmailCliente_wizard() != null) {
                                EmailCliente emailCliente = new EmailCliente(getEmailCliente_wizard());
                                emailCliente.setCliente(cliente);
                                current.setEmailCliente(emailCliente);
                            }

                        }

                        setEmailCliente_wizard_existeEmail(false);
                        setEmailCliente_wizard_existeCliente(false);

                    }

                } else {
                    getSelected().getEmailCliente().getCliente().setRut(rutCliente_wizard);
                }

            }

        } catch (Exception ex) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "formateaRutFiltro2", ex);
        }
    }

    private void persist(Caso newCaso) throws PreexistingEntityException, RollbackFailureException, Exception {

        TipoAlerta alerta = EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta();

        if (alerta != null) {
            newCaso.setEstadoAlerta(alerta);
        }

        if (emailCliente_wizard_updateCliente && emailCliente_wizard_existeCliente) {
            getJpaController().merge(newCaso.getEmailCliente().getCliente());
        }

        if (!emailCliente_wizard_existeEmail) {
            getJpaController().persist(newCaso.getEmailCliente());
        }

        newCaso.setIdCanal(EnumCanal.INTERNO.getCanal());

        getManagerCasos().persistCaso(newCaso, getManagerCasos().createLogComment(newCaso, "Se crea caso manualmente"));
//        getManagerCasos().agendarAlertas(newCaso);
        JsfUtil.addSuccessMessage("El Caso " + newCaso.getIdCaso() + " ha sido creado con éxito.");

    }

    public void verifyGoToCaso() {
        HttpServletRequest request = (HttpServletRequest) JsfUtil.getRequest();
        String id = request.getParameter("id");
        if (id != null) {
            try {
                Caso casoRequested = getJpaController().find(Caso.class, Long.parseLong(id));
                if (casoRequested == null) {
                    JsfUtil.addErrorMessage("Caso ID " + id + " no existe en el sistema");
                    FacesContext.getCurrentInstance().getExternalContext().redirect("../index.xhtml");
                } else {
                    setSelected(casoRequested);
                    FacesContext.getCurrentInstance().getExternalContext().redirect("Edit.xhtml");
                }

            } catch (Exception ex) {
                JsfUtil.addErrorMessage("Sesion invalida");
                Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        //return null;
    }

    public void actualizaArbolDeCategoria() {
//        //System.out.println("actualizaArbolDeCategoria");
        Object res = JsfUtil.getManagedBean("categoriaController");
        if (res != null) {
            CategoriaController catController = ((CategoriaController) res);
            catController.filtrarCategorias();
        }
    }

    public void actualizaArbolDeCategoria(EventListener event) {
//        //System.out.println("actualizaArbolDeCategoria");
        Object res = JsfUtil.getManagedBean("categoriaController");
        if (res != null) {
            CategoriaController catController = ((CategoriaController) res);
            catController.filtrarCategorias();
        }
    }

    public void onNodeSelect(NodeSelectEvent event) {
        Object res = JsfUtil.getManagedBean("categoriaController");
        if (res != null) {
            try {
                CategoriaController catController = ((CategoriaController) res);
                Categoria catSelected = (Categoria) catController.getCategoria().getData();
//            //System.out.println("categoria " + catSelected + " seleccionada");
                current.setIdCategoria(catSelected);

                getJpaController().merge(current);
            } catch (Exception ex) {
                Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "error at onNodeSelect", ex);
            }
        }
    }

    public boolean getIncluirHistoria() {
        return incluirHistoria;
    }

    public void setIncluirHistoria(boolean incluirHistoria) {
        this.incluirHistoria = incluirHistoria;
    }

    public String getIdFileDelete() {
        return idFileDelete;
    }

    public Long getIdFileRemove() {
        return idFileRemove;
    }

    public void setIdFileDelete(String idFileDelete) {
        this.idFileDelete = idFileDelete;
        //System.out.println("idFileDelete:" + idFileDelete);
    }
    /**
     * Id Caso relacionado
     */
    private String idCaserel = "";

    @Override
    public JPAServiceFacade getJpaController() {
        if (jpaController == null) {
            jpaController = super.getJpaController();
            RulesEngine rulesEngine = new RulesEngine(super.getJpaController());
            rulesEngine.setApplicationBean(applicationBean);
            rulesEngine.setUserSessionBean(userSessionBean);
            jpaController.setCasoChangeListener(rulesEngine);
        }
        return jpaController;
    }

    public String getIdCaserel() {
        return idCaserel;
    }

    public void setIdCaserel(String idCaserel) {
        this.idCaserel = idCaserel;
    }

    public Long getIdCaso() {
        return idCaso;
    }

    public void setIdCaso(Long idCaso) {
        this.idCaso = idCaso;
    }

    public String creaAutorDeNota(Nota nota) {
        if (nota == null) {
            return "Anónimo";
        }
        if (EnumTipoNota.RESPUESTA_DE_CLIENTE.getTipoNota().equals(nota.getTipoNota())) {
            if (nota.getEnviadoPor() == null) {
                return "Anónimo";
            } else {
                EmailCliente e = getJpaController().find(EmailCliente.class, nota.getEnviadoPor());
                if (e != null) {
                    if (e.getCliente() != null) {
                        return e.getCliente().getCapitalName();
                    }
                }
                return nota.getEnviadoPor();
            }
        }
        return nota.getCreadaPor().getCapitalName();
    }

    public String creaTituloDeNota(Nota nota) {
//        SimpleDateFormat format = new SimpleDateFormat("EEE, dd/MM/yyyy HH:mm");
        if (nota != null && nota.getTipoNota() != null) {
            if (EnumTipoNota.RESPUESTA_DE_CLIENTE.getTipoNota().equals(nota.getTipoNota())) {
                if (nota.getEnviadoPor() != null) {
                    return nota.getEnviadoPor();
                }
            }
            return nota.getCreadaPor().getCapitalName();

        }

        return "Autor desconocido";

    }

//    public int cantidadAttachment() {
//        try {
//            int count = 0;
//            for (Attachment attachment : getSelected().getAttachmentList()) {
//                if (attachment.getContentId() == null) {
//                    count++;
//                }
//            }
//            return count;// getJpaController().countAttachmentsWOContentId(current).intValue();
//        } catch (Exception ex) {
//            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "Error at casoController.cantidadAttachment", ex);
//            return 0;
//        }
//    }
//
//    public int cantidadAttachmentEmbedded() {
////        return cantidadAttachmentsEmbedded;//getJpaController().countAttachmentWContentId(current).intValue();
//         try {
//            int count = 0;
//            for (Attachment attachment : getSelected().getAttachmentList()) {
//                if (attachment.getContentId() != null) {
//                    count++;
//                }
//            }
//            return count;// getJpaController().countAttachmentsWOContentId(current).intValue();
//        } catch (Exception ex) {
//            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "Error at casoController.cantidadAttachmentEmbedded", ex);
//            return 0;
//        }
//    }
    public String nombreArchivoParaDesplegar(String nombreOriginal) {
        int max = 25;
        if (nombreOriginal.length() >= max) {
            StringBuilder sbuilder = new StringBuilder(nombreOriginal.substring(0, max - 3));
            sbuilder.append("...");
            return sbuilder.toString().toLowerCase();
        }
        return nombreOriginal.toLowerCase();
    }

//    public Collection datosAdjuntos() {
//        try {
//            return current.getAttachmentList();
//        } catch (Exception e) {
//            return new LinkedList();
//        }
//    }
//    public List<Attachment> datosAdjuntosNotEmbedded() {
//        return attachmentWOContentId;
//    }
//    public void formateaRutFiltro() {
//        String rutFormateado = UtilesRut.formatear(getCaso().getRutCliente());
//        getCaso().setRutCliente(rutFormateado);
//        if (!getCaso().getRutCliente().isEmpty()) {
//            if (!UtilesRut.validar(getCaso().getRutCliente())) {
//                JsfUtil.addErrorMessage("Rut invalido");
//            }
//        }
//    }
    public void formateaRut() {
        String rutFormateado = UtilesRut.formatear(getSelected().getEmailCliente().getCliente().getRut());
        getSelected().getEmailCliente().getCliente().setRut(rutFormateado);
        validaRut();
    }

    public void validaRut() {
        if (!UtilesRut.validar(getSelected().getEmailCliente().getCliente().getRut())) {
            JsfUtil.addErrorMessage("Rut invalido");
        }
    }

    public boolean esColaborativo() {
        return current.getIdCanal().equals(EnumCanal.INTERNO.getCanal());
    }

    public String parseHtmlToText(String textoTxt) {
        textoTxt = HtmlUtils.stripInvalidMarkup(textoTxt);
        return textoTxt;
    }

    public String parseHtmlToTextPreview(String textoTxt) {
        textoTxt = HtmlUtils.stripInvalidMarkup(textoTxt);
        int endIndex = textoTxt != null ? textoTxt.length() : 0;
        endIndex = (endIndex < 30) ? endIndex : 30;
        return endIndex != 0 ? textoTxt.substring(0, endIndex) : textoTxt;
    }

    public String htmlToReducedSecureView(String textHtml) {
        String textoTxt = HtmlUtils.stripInvalidMarkup(textHtml);
        if (textoTxt.length() > 10000) {
            return textoTxt.substring(0, 500);
        }
        return textoTxt;
    }

    public void prepareViewHtml(String html) {
        htmlToView = replaceEmbeddedImages(html);
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("modal", true);
        options.put("draggable", false);
        options.put("resizable", true);
        options.put("contentHeight", 500);
        options.put("contentWidth", 600);
        RequestContext.getCurrentInstance().openDialog("/script/caso/ViewHtml", options, null);
    }

//    public String prepareViewHtml(String html) {
//        htmlToView = replaceEmbeddedImages(html);
//        return "/script/caso/ViewHtml";
//    }
    private String replaceEmbeddedImages(String html) {
        String pattern = "src=\"cid:";
        int index = html.indexOf(pattern);
        int endIndex = 0;
        while (index > -1) {
            index += pattern.length();
            endIndex = html.indexOf("\"", index);
            String contentId = html.substring(index, endIndex);
            String base64Image = createEmbeddedImage(contentId);
            if (base64Image != null) {
                html = html.replace("cid:" + contentId, "data:image/png;base64," + base64Image);
            }
            index = html.indexOf("src=\"cid:", endIndex);
        }
        //src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIA..."
        return html;
    }

    public String getTextoNota() {
        return textoNota;
    }

    public void setTextoNota(String textoNota) {
        this.textoNota = textoNota;
    }

    public List<Caso> getCasosPendientes() {
        try {
            return (List<Caso>) getJpaController().findAllEntities(Caso.class, applicationBean.getVistaPorAlerta(EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta()),
                    getDefaultOrderBy(), userSessionBean.getCurrent());
        } catch (Exception ex) {
            addErrorMessage(ex.getMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "getCasosPendientes error", ex);
            return Collections.EMPTY_LIST;
        }
    }

    public List<Caso> getCasosPorVencer() {
        try {
            return (List<Caso>) getJpaController().findAllEntities(Caso.class, applicationBean.getVistaPorAlerta(EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta()),
                    getDefaultOrderBy(), userSessionBean.getCurrent());
        } catch (Exception ex) {
            addErrorMessage(ex.getMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "getCasosPorVencer error", ex);
            return Collections.EMPTY_LIST;
        }
    }

    public List<Caso> getCasosVencidos() {
        try {
            return (List<Caso>) getJpaController().findAllEntities(Caso.class, applicationBean.getVistaPorAlerta(EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta()),
                    getDefaultOrderBy(), userSessionBean.getCurrent());
        } catch (Exception ex) {
            addErrorMessage(ex.getMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "getCasosVencidos error", ex);
            return Collections.EMPTY_LIST;
        }
    }

    public String getCasosPendientesCount() {
        try {
            return getJpaController().countEntities(applicationBean.getVistaPorAlerta(EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta()), userSessionBean.getCurrent()).toString();
        } catch (ClassNotFoundException ex) {
            addErrorMessage(ex.getMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "getCasosPendientes error", ex);
            return "0";
        }
    }

    public String getCasosPorVencerCount() {
        try {
            return getJpaController().countEntities(applicationBean.getVistaPorAlerta(EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta()), userSessionBean.getCurrent()).toString();
        } catch (ClassNotFoundException ex) {
            addErrorMessage(ex.getMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "getCasosPendientes error", ex);
            return "0";
        }
//        return getJpaController().getCasoCount(userSessionBean.getCurrent(), EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta()) + "";
    }

    public String getCasosVencidoCount() {
        try {
            return getJpaController().countEntities(applicationBean.getVistaPorAlerta(EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta()), userSessionBean.getCurrent()).toString();
        } catch (ClassNotFoundException ex) {
            addErrorMessage(ex.getMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "getCasosPendientes error", ex);
            return "0";
        }
//        return getJpaController().getCasoCount(userSessionBean.getCurrent(), EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta()) + "";
    }

    public String getCountCasosRevisarActualizacion() {
        try {
            return getJpaController().countEntities(applicationBean.getVistaRevisarActualizacion(), userSessionBean.getCurrent()).toString();
        } catch (ClassNotFoundException ex) {
            addErrorMessage(ex.getMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "getCasosPendientes error", ex);
            return "0";
        }
//        return getJpaController().getCasoCountActualizados(userSessionBean.getCurrent()) + "";
    }

    public int getCantidadDeNotas() {
        cantidadDeNotas = 0;
        cantidadDeRespuestasACliente = 0;
        cantidadDeRespuestasDelCliente = 0;
        if (current != null) {
            if (listaActividadesOrdenada != null) {
                for (Nota nota : listaActividadesOrdenada) {
                    if ((nota.getTipoNota() != null) && nota.getTipoNota().equals(EnumTipoNota.NOTA.getTipoNota())) {
                        cantidadDeNotas++;
                    }
                    if ((nota.getTipoNota() != null) && nota.getTipoNota().equals(EnumTipoNota.RESPUESTA_A_CLIENTE.getTipoNota())) {
                        cantidadDeRespuestasACliente++;
                    }
                    if ((nota.getTipoNota() != null) && nota.getTipoNota().equals(EnumTipoNota.RESPUESTA_DE_CLIENTE.getTipoNota())) {
                        cantidadDeRespuestasDelCliente++;
                    }
                }
            }

        }

        return cantidadDeNotas;
    }

    public boolean notaEditable(Nota nota) {
        return EnumTipoNota.NOTA.getTipoNota().equals(nota.getTipoNota())
                && nota.getCreadaPor() != null
                && nota.getCreadaPor().equals(userSessionBean.getCurrent());
    }

    public String prettyDate(Date date) {
        if (date != null) {
            return PrettyDate.format(date);
        } else {
            return "";
        }
    }

    public String formatDate(Date date) {
        if (date != null) {
            return fullDateFormat.format(date);
        } else {
            return "";
        }
    }

    public String getCanalStyleClass(Canal canal) {
        if (canal != null) {
            if (canal.equals(EnumCanal.CHAT.getCanal())) {
                return "fa-comments";
            } else if (canal.equals(EnumCanal.CORREO.getCanal())) {
                return "fa-envelope";
            } else if (canal.equals(EnumCanal.PHONE.getCanal())) {
                return "fa-phone";
            } else if (canal.equals(EnumCanal.INTERNO.getCanal())) {
                return "fa-ticket";
            } else if (canal.equals(EnumCanal.PORTAL.getCanal())) {
                return "fa-comment";
            } else if (canal.equals(EnumCanal.WEBSERVICE.getCanal())) {
                return "fa-cloud";
            }
        }

        return "fa-exclamation";
    }

    public String getNotaStyle(TipoNota tipoNota) {
        for (EnumTipoNota enumTipoNota : EnumTipoNota.values()) {
            if (enumTipoNota.getTipoNota().equals(tipoNota)) {
                return enumTipoNota.getStyle();
            }
        }
        return "";
    }

    public String prepareList() {
        recreateModel();
        return "inbox";
    }

    public void handleFileUpload(FileUploadEvent event) {

        FacesMessage msg = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public String onFlowProcess(FlowEvent event) {
        return event.getNewStep();
    }

    public String crearCasoColab(Caso casoTmp) {
        try {
            current = new Caso();
            current.setIdCategoria(null);
            current.setRevisarActualizacion(false);
            current.setIdCasoPadre(casoTmp);
            Usuario usr = casoTmp.getOwner();
            EmailCliente cliente = getJpaController().find(EmailCliente.class, usr.getEmail());
            if (cliente != null) {
                current.setEmailCliente(cliente);
            } else {
                EmailCliente newCliente = new EmailCliente(usr.getEmail());
                Cliente cliente_record = new Cliente();
                cliente_record.setRut(usr.getRut());
                cliente_record.setNombres(usr.getNombres());
                cliente_record.setApellidos(usr.getApellidos());
                cliente_record.setFono1(usr.getTelFijo());
                newCliente.setCliente(cliente_record);
//                getJpaController().persistEmailCliente(newCliente);
                current.setEmailCliente(newCliente);

            }

            current.setTema("[Colab] " + casoTmp.getTema());
            current.setIdCanal(EnumCanal.INTERNO.getCanal());
            current.setFechaCreacion(Calendar.getInstance().getTime());
            current.setFechaModif(current.getFechaCreacion());
            current.setOwner(null);
            EstadoCaso ec = new EstadoCaso();
            ec.setIdEstado(EnumEstadoCaso.ABIERTO.getEstado().getIdEstado());

            current.setTipoCaso(EnumTipoCaso.INTERNO.getTipoCaso());
            current.setIdSubEstado(EnumSubEstadoCaso.INTERNO_NUEVO.getSubEstado());
            current.setIdPrioridad(getJpaController().find(Prioridad.class, EnumPrioridad.MEDIA.getPrioridad().getIdPrioridad()));

            current.setIdEstado(ec);
            selectedItemIndex = -1;
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).logInfo("No se ha podido generar un caso interno");
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            //e.printStackTrace();
        }
        return "/script/caso/Create";
    }

    public String prepareCreate() {
        try {
            current = new Caso();
//            current.setIdCategoria(null);
//            current.setRevisarActualizacion(true);
//            current.setIdPrioridad(null);
//            current.setFechaCreacion(Calendar.getInstance().getTime());
//            current.setFechaModif(current.getFechaCreacion());
            current.setTipoCaso(EnumTipoCaso.CONTACTO.getTipoCaso());
            current.setOwner(userSessionBean.getCurrent());
            EmailCliente email = new EmailCliente();
            email.setCliente(new Cliente());
            current.setEmailCliente(email);
//            EstadoCaso ec = new EstadoCaso();
//            ec.setIdEstado(EnumEstadoCaso.ABIERTO.getEstado().getIdEstado());

//            SubEstadoCaso sec = new SubEstadoCaso();
//            sec.setIdSubEstado(EnumSubEstadoCaso.NUEVO.getSubEstado().getIdSubEstado());
//            current.setIdSubEstado(sec);
//            current.setIdEstado(ec);
            emailCliente_wizard_existeEmail = false;
            emailCliente_wizard_existeCliente = false;
            emailCliente_wizard_updateCliente = false;
            emailCliente_wizard = null;
            rutCliente_wizard = null;
            selectedItemIndex = -1;
            stepNewCasoIndex = 1;//2,3 o 4.
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).logInfo("Error al preparar la creacion de un caso");
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return "/script/caso/Create";
    }

    public String prepareCreateCasoFromCustomer() {
        try {
            current = new Caso();

            if (userSessionBean.isValidatedCustomerSession()) {
                emailCliente_wizard = userSessionBean.getEmailCliente().getEmailCliente();
                current.setEmailCliente(userSessionBean.getEmailCliente());
                emailCliente_wizard_existeEmail = true;
                if (userSessionBean.getEmailCliente().getCliente() != null) {
                    emailCliente_wizard_existeCliente = true;
                    if (StringUtils.isEmpty(userSessionBean.getEmailCliente().getCliente().getRut())) {
                        rutCliente_wizard = null;
                    } else {
                        rutCliente_wizard = userSessionBean.getEmailCliente().getCliente().getRut();
                    }
                } else {
                    emailCliente_wizard_existeCliente = false;
                }
            } else {
                EmailCliente email = new EmailCliente();
                email.setCliente(new Cliente());
                current.setEmailCliente(email);
                emailCliente_wizard_existeEmail = false;
                emailCliente_wizard = null;
            }

            emailCliente_wizard_updateCliente = false;
            selectedItemIndex = -1;
            stepNewCasoIndex = 1;//2,3 o 4.

        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).logInfo("Error al preparar la creacion de un caso");
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        return "/customer/newSR";
    }

    public String createCasoCustomerStep() {

        switch (stepNewCasoIndex) {
            case 1:

                EmailCliente email = getJpaController().find(EmailCliente.class, emailCliente_wizard);
                if (email != null) {
                    if (email.getCliente() == null) {
                        email.setCliente(new Cliente());
                        emailCliente_wizard_updateCliente = false;
                        emailCliente_wizard_existeCliente = false;
                    } else {
                        List<ProductoContratado> prods = email.getCliente().getProductoContratadoList();
                        if (prods != null && !prods.isEmpty()) {
                            ProductoContratado pc = prods.get(0);
                            current.setIdProducto(pc.getProducto());
                            current.setIdComponente(pc.getComponente());
                            current.setIdSubComponente(pc.getSubComponente());
                        }
                        emailCliente_wizard_updateCliente = true;
                        emailCliente_wizard_existeCliente = true;
                    }
                    emailCliente_wizard_existeEmail = true;
                } else {
                    emailCliente_wizard_existeEmail = false;
                    emailCliente_wizard_updateCliente = false;
                    emailCliente_wizard_existeCliente = false;
                    email = new EmailCliente(emailCliente_wizard);
                    email.setCliente(new Cliente());
                }

                current.setEmailCliente(email);

//                if(!emailCliente_wizard.equalsIgnoreCase(userSessionBean.getEmailCliente().getEmailCliente())){
//                    //email entered not customer email.
//                    
//                }
                stepNewCasoIndex++;
                break;
            case 2:
                stepNewCasoIndex++;
                break;
            case 3:
                stepNewCasoIndex++;
                break;
            case 4:
                return createAndCustomerView();

        }

        return null;

    }

    public String prepareCopy() {
        return prepareCopy(current, false);
    }

    public String prepareCopyAndAssignMe() {
        return prepareCopy(current, true);
    }

    private String prepareCopy(Caso origin, boolean assignme) {
        try {
            Caso copy = new Caso();
            copy.setIdCategoria(origin.getIdCategoria());
            copy.setDescripcion(origin.getDescripcion());
            copy.setDescripcionTxt(origin.getDescripcionTxt());
            copy.setEmailCliente(origin.getEmailCliente());
            copy.setEsPregConocida(origin.getEsPregConocida());
            copy.setEsPrioritario(origin.getEsPrioritario());
            copy.setEstadoAlerta(origin.getEstadoAlerta());
            copy.setEstadoEscalacion(origin.getEstadoEscalacion());

            copy.setIdComponente(origin.getIdComponente());
            copy.setIdPrioridad(origin.getIdPrioridad());
            copy.setIdProducto(origin.getIdProducto());
            copy.setIdSubComponente(origin.getIdSubComponente());
            copy.setIdCasoPadre(origin.getIdCasoPadre());

            copy.setTema(origin.getTema());
            copy.setTranferCount(0);

            copy.setRevisarActualizacion(true);
            copy.setFechaCreacion(Calendar.getInstance().getTime());
            copy.setFechaModif(copy.getFechaCreacion());

            copy.setTipoCaso(origin.getTipoCaso());
            copy.setIdSubEstado(origin.getIdSubEstado());
            copy.setIdEstado(origin.getIdEstado());
            copy.setEtiquetaStringList(origin.getEtiquetaStringList());

            if (origin.getEmailCliente() != null) {
                emailCliente_wizard_existeEmail = true;
                emailCliente_wizard = origin.getEmailCliente().getEmailCliente();
                try {
                    rutCliente_wizard = origin.getEmailCliente().getCliente().getRut();
                } catch (Exception e) {
                    //ignore.
                }
            }
            if (origin.getEmailCliente() != null && origin.getEmailCliente().getCliente() != null) {
                emailCliente_wizard_existeCliente = true;
            }

            if (assignme) {
                copy.setOwner(userSessionBean.getCurrent());
            } else {
                copy.setOwner(origin.getOwner());
            }

            copy.setIdCanal(EnumCanal.INTERNO.getCanal());

            current = copy;

        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).logInfo("Error al preparar la creacion (copia) de un caso");
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
        selectedItemIndex = -1;
        return "/script/caso/Create";
    }

    /*TODO deprecate this code*/
    public void prepareCreateNotaGenerica(ActionEvent actionEvent) {
        textoNota = null;
    }

    public void goToNextStepMobile() {
    }

    public void createCaso() {
        try {
            persist(current);
            prepareDynamicCaseData(current);
        } catch (PreexistingEntityException ex) {
            JsfUtil.addErrorMessage("El Caso no pudo ser creado, existe otro caso con el mismo id.");
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RollbackFailureException ex) {
            JsfUtil.addErrorMessage("El Caso no pudo ser creado, hay un problema con integridad de los datos..");
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("El Caso no pudo ser creado:" + ex.getLocalizedMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String createAndView() {
        try {
            persist(current);
            prepareDynamicCaseData(current);
            return "/script/caso/Edit";
        } catch (PreexistingEntityException e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage("El Caso con id " + current.getIdCaso() + " ya existe!");
            return null;
        } catch (RollbackFailureException e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, "Se produjo una inconsistencia de datos, favor intente mas tarde.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.createLogger(this.getClass().getName()).logInfo(resourceBundle.getString("PersistenceErrorOccured"));
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
//            JsfUtil.addErrorMessage(e, resourceBundle.getString("PersistenceErrorOccured"));
            JsfUtil.addErrorMessage(e, "Lo sentimos, en este momento tenemos un problema de inconsistencia de datos, favor intente más tarde o envíenos un correo al área correspondiente.");
            return null;
        }

    }

    public String createAndCustomerView() {
        try {
            current.setIdCanal(EnumCanal.PORTAL.getCanal());
            persist(current);
            prepareDynamicCaseData(current);
//            listaActividadesOrdenada = null;
            userSessionBean.setEmailCliente(current.getEmailCliente());
            userSessionBean.setCurrent(null);
            prepareCasoFilterForInbox();
            return "/customer/ticket";
        } catch (PreexistingEntityException e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage("El Caso con id " + current.getIdCaso() + " ya existe!");
            return null;
        } catch (RollbackFailureException e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, "Se produjo una inconsistencia de datos, favor intente mas tarde.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.createLogger(this.getClass().getName()).logInfo(resourceBundle.getString("PersistenceErrorOccured"));
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
//            JsfUtil.addErrorMessage(e, resourceBundle.getString("PersistenceErrorOccured"));
            JsfUtil.addErrorMessage(e, "Lo sentimos, en este momento tenemos un problema de inconsistencia de datos, favor intente más tarde o envíenos un correo al área correspondiente.");
            return null;
        }

    }

    public void refreshCaso() throws Exception {
        current = getJpaController().find(Caso.class, current.getIdCaso());
        prepareDynamicCaseData(current);
//        recreateModel();
    }

    public Caso getCurrentCaso() {
        return current;
    }

    public void prepareEditNota(Integer idNota) throws Exception {
        for (Nota nota : current.getNotaList()) {
            if (nota.getIdNota().equals(idNota)) {
                selectedNota = nota;
            }
        }
    }

    public String prepareCustomerViewCaso(Caso casoSearch) {
        try {
            if (casoSearch == null) {
                JsfUtil.addErrorMessage("Caso " + casoSearch.getIdCaso() + " no encontrado");
            } else {
                current = getJpaController().find(Caso.class, casoSearch.getIdCaso());
                prepareDynamicCaseData(current);
            }

            if (userSessionBean.isValidatedCustomerSession()) {
                return "/customer/ticket";
            } else if (userSessionBean.isValidatedSession()) {
                return "/script/caso/Edit";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JsfUtil.addErrorMessage("Caso " + casoSearch.getIdCaso() + " no encontrado." + ex.getMessage());
        }
        return null;
    }

    public String prepareEdit(Caso caso) throws Exception {
        current = caso;
        prepareDynamicCaseData(current);

        if (userSessionBean.isValidatedCustomerSession()) {
            return "/customer/ticket";
        } else if (userSessionBean.isValidatedSession()) {
            return "/script/caso/Edit";
        } else {
            return null;
        }
    }

    private void prepareEditIndex(Integer index) throws Exception {
        if (index != null) {
            selectedItemIndex = index;
            getItems().setRowIndex(index);
            current = (Caso) getItems().getRowData();
        } else {
            current = (Caso) getItems().getRowData();
            selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        }

        if ((current.getOwner() != null)
                && (current.getOwner().getIdUsuario().equals(userSessionBean.getCurrent().getIdUsuario()))) {
            if (current.getRevisarActualizacion()) {
                current.setRevisarActualizacion(false);
                List<AuditLog> changeLog = new ArrayList<AuditLog>();
                changeLog.add(getManagerCasos().createLogComment(current, "Agente propietario del caso revisa caso pendiente de revisión."));
                getJpaController().mergeCaso(current, changeLog);
            }
        }

        prepareDynamicCaseData(current);

    }

    public String prepareEditPreviusItem() throws Exception {

        if (selectedItemIndex > pagination.getPageFirstItem()) {
            prepareEditIndex(--selectedItemIndex);
        } else {
            prepareEditIndex(pagination.getPageFirstItem());
        }

        if (userSessionBean.isValidatedCustomerSession()) {
            return "/customer/ticket";
        } else if (userSessionBean.isValidatedSession()) {
            return "/script/caso/Edit";
        } else {
            return null;
        }
    }

    public String prepareEditNextItem() throws Exception {

        if (selectedItemIndex < pagination.getPageLastItem()) {
            prepareEditIndex(++selectedItemIndex);
        } else {
            prepareEditIndex(pagination.getPageFirstItem());
        }

        if (userSessionBean.isValidatedCustomerSession()) {
            return "/customer/ticket";
        } else if (userSessionBean.isValidatedSession()) {
            return "/script/caso/Edit";
        } else {
            return null;
        }
    }

    public String prepareEdit() throws Exception {
        prepareEditIndex(null);
        if (userSessionBean.isValidatedCustomerSession()) {
            return "/customer/ticket";
        } else if (userSessionBean.isValidatedSession()) {
            return "/script/caso/Edit";
        } else {
            return null;
        }
    }

    /**
     * Search ticket id in header
     *
     * @return
     */
    public String prepareEditCaso() {
        if (idCaso == null) {
            JsfUtil.addErrorMessage("Debe ingresar un número de caso.");
            return "";
        } else {
            try {
                Caso casoSearch = getJpaController().find(Caso.class, idCaso);
                if (casoSearch == null) {
                    JsfUtil.addErrorMessage("Caso " + idCaso + " no encontrado");
                    return "";
                } else {
                    setSelected(casoSearch);
                    prepareDynamicCaseData(current);
                    if (userSessionBean.isValidatedCustomerSession()) {
                        return "/customer/ticket";
                    } else if (userSessionBean.isValidatedSession()) {
                        return "/script/caso/Edit";
                    } else {
                        return null;
                    }
                }
            } catch (Exception ex) {
                JsfUtil.addErrorMessage("Caso " + idCaso + " no encontrado");
                return "";
            }
        }
    }

    public String filterByIdCaso(Long idCaso) {

        current = getJpaController().find(Caso.class, idCaso);
//        listaActividadesOrdenada = null;
        prepareDynamicCaseData(current);

        if (current != null) {
            return "/script/caso/Edit";
        } else {
            return null;
        }
    }

//    public String redirect(String dir) {
//        return dir;
//    }
    public String filterList() {
        categoria = null;
        recreateModel();
        return "inbox";
    }

    public void onTagIdSelectCaso(String idTag) {
        selectThisTag(idTag);
        redirect("/script/index.xhtml");
    }

    public void onTagSelectCaso(SelectEvent event) {
        onTagSelect(event);
        redirect("/script/index.xhtml");
    }

    /**
     * @deprecated @param event
     */
    public void onTagSelect(SelectEvent event) {
        TagCloudItem item = (TagCloudItem) event.getObject();
        selectThisTag(item.getLabel());
    }

    public void onEtiquetaSelected(String e) {
        selectThisTag(e);
    }

    private void selectThisTag(String tagId) {
        Vista copy = new Vista(Caso.class);
        copy.setNombre(tagId);

        FiltroVista fCopy = new FiltroVista();
        fCopy.setIdFiltro(1);
        fCopy.setIdCampo("etiquetaList");
        fCopy.setIdComparador(EnumTipoComparacion.IM.getTipoComparacion());
        fCopy.setValor(tagId);
        fCopy.setIdVista(copy);

        copy.getFiltrosVistaList().add(fCopy);

        getFilterHelper().setVista(copy);
        pagination = null;

        recreateModel();

//        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Etiqueta Seleccionada:" + tagId, "");
//        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public Vista getVista() {
        return getFilterHelper().getVista();
    }

    public void setVista(Vista vista) {
        getFilterHelper().setVista(vista);
        pagination = null;
        recreateModel();
    }

    @Override
    public List<Caso> getSelectedItems() {
        List<Caso> selected = new LinkedList<Caso>();
        for (Caso object : (List<Caso>) getItems().getWrappedData()) {
            if (object.isSelected()) {
                selected.add(object);
            }
        }
        return selected;
    }

    public void applyReglaToSelectedCasos() {
        if (getSelectedItemsCount() > 0) {
            RulesEngine rulesEngine = new RulesEngine(super.getJpaController());
            rulesEngine.applyRuleOnThisCasos(reglaTriggerSelected, getSelectedItems());
            addInfoMessage("Regla " + reglaTriggerSelected + " ejecutada en " + getSelectedItemsCount() + " casos.");
        } else {
            addWarnMessage("Seleccione los casos a los cuales desea ejecutar la regla de negocio.");
        }
    }

    public void runActionOnSelectedCasos() {

        System.out.println("accionToRunSelected:" + accionToRunSelected);
        Action a = applicationBean.getPredefinedActions().get(accionToRunSelected);
        a.setConfig(accionToRunParametros);

        List<Caso> casosToSend = Collections.EMPTY_LIST;
        if (getSelectedItemsCount() > 0) {
            casosToSend = getSelectedItems();
        } else {
            //run on all items in Vista
            try {
                casosToSend = (List<Caso>) getJpaController().findAllEntities(Caso.class, getFilterHelper().getVista(), getDefaultOrderBy(), userSessionBean.getCurrent());
            } catch (Exception ex) {
                Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "findEntities", ex);
            }
        }

        int count = 0;
        for (Caso caso : casosToSend) {
            try {
                a.execute(caso, getJpaController().getSchema());
                count++;
            } catch (ActionExecutionException ex) {
                Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        addInfoMessage("Acción " + accionToRunSelected + " ejecutada exitosamente en " + count + " casos.");
        if (getSelectedItemsCount() > count) {
            addWarnMessage("Error: " + (getSelectedItemsCount() - count) + " casos no han sido enviados, favor revisar la configuración de correo del area correspondiente.");
        }

    }

    public void applyVista(Vista vista) {

        System.out.println("applyVista..." + vista);
        this.selectedViewId = vista.getIdVista();
        getFilterHelper().setVista(vista);
        recreateModel();

//        try {
//            Vista copy = new Vista(Caso.class);
//            copy.setIdUsuarioCreadaPor(userSessionBean.getCurrent());
//            copy.setDescripcion(vista.getDescripcion());
//            copy.setIdArea(vista.getIdArea());
//            copy.setIdGrupo(vista.getIdGrupo());
//            copy.setNombre(vista.getNombre());
//            copy.setVisibleToAll(vista.getVisibleToAll());
//            //Crearemos una copia para que al guardar no pisar la existente.
//            int i = 1;
//            if (vista.getFiltrosVistaList() != null) {
//                for (FiltroVista f : vista.getFiltrosVistaList()) {
//                    FiltroVista fCopy = new FiltroVista();
//                    fCopy.setIdFiltro(i);
//                    i++; //This is an ugly patch to solve issue when removing a filter from the view, if TODO: Warning - this method won't work in the case the id fields are not set
//                    fCopy.setIdCampo(f.getIdCampo());
//                    fCopy.setIdComparador(f.getIdComparador());
//                    fCopy.setValor(f.getValor());
//                    fCopy.setValor2(f.getValor2());
//                    fCopy.setIdVista(copy);
//                    copy.getFiltrosVistaList().add(fCopy);
//                }
//            }
//
//            getFilterHelper().setVista(copy);
//            recreateModel();
//        } catch (Exception e) {
//            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "error applyVista", e);
//        }
    }

    public String applyViewFilter(Vista vista) {

        applyVista(vista);

        if (userSessionBean.isValidatedCustomerSession()) {
            return "inbox_customer";
        } else if (userSessionBean.isValidatedSession()) {
            return "inbox";
        } else {
            return null;
        }

    }

    public void prepareGuardarVista() {
        JsfUtil.addWarningMessage("Atención: Al Guardar se creará una vista Nueva.");
    }

    public String saveCurrentView() {
        //SE hizo una copia de esta vista, y los filtros tienen ids falsos
        for (FiltroVista f : getFilterHelper().getVista().getFiltrosVistaList()) {
            f.setIdFiltro(null); //This is an ugly patch to solve issue when removing a filter from the view, if TODO: Warning - this method won't work in the case the id fields are not set
        }
        try {
            vistaController.create(getFilterHelper().getVista());
            executeInClient("saveViewDialog.hide()");
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "ERROR AT saveCurrentView", e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
        return null;
    }

    public void prepareCasoFilterForInbox() {

        List<Vista> vistas = vistaController.getAllVistasItems();

        if (vistas != null && !vistas.isEmpty()) {
            final Vista vista0 = vistas.get(0);
            //there are vistas, lets select the first one as the user inbox
            getFilterHelper().setVista(vista0);
            setSelectedViewId(vista0.getIdVista());
        } else {
            Vista vista1 = new Vista(Caso.class);
            vista1.setIdVista(1);
            vista1.setNombre("Inbox");

            if (userSessionBean.isValidatedCustomerSession()) {
                //customer view!
//            vista1.setIdUsuarioCreadaPor(EnumUsuariosBase.SISTEMA.getUsuario());

                FiltroVista filtroEmailCliente = new FiltroVista();
                filtroEmailCliente.setIdFiltro(1);//otherwise i dont know what to remove dude.
                filtroEmailCliente.setIdCampo("emailCliente");
                filtroEmailCliente.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
                filtroEmailCliente.setValor(userSessionBean.getEmailCliente().getEmailCliente());
                filtroEmailCliente.setIdVista(vista1);

                vista1.getFiltrosVistaList().add(filtroEmailCliente);

            } else if (userSessionBean.isValidatedSession()) {
                vista1.setIdUsuarioCreadaPor(userSessionBean.getCurrent());

                FiltroVista filtroOwner = new FiltroVista();
                filtroOwner.setIdFiltro(1);//otherwise i dont know what to remove dude.
                filtroOwner.setIdCampo("owner");
                filtroOwner.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
                filtroOwner.setValor(AbstractJPAController.PLACE_HOLDER_CURRENT_USER);
                filtroOwner.setIdVista(vista1);

                vista1.getFiltrosVistaList().add(filtroOwner);

            } else {
//            System.out.println("FUCK!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//            System.out.println(userSessionBean.getCurrent());
//            System.out.println(userSessionBean.getEmailCliente());
            }

            FiltroVista filtroEstado = new FiltroVista();
            filtroEstado.setIdFiltro(2);//otherwise i dont know what to remove dude.
            filtroEstado.setIdCampo("idEstado");
            filtroEstado.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
            filtroEstado.setValor(EnumEstadoCaso.ABIERTO.getEstado().getIdEstado());
            filtroEstado.setIdVista(vista1);

            vista1.getFiltrosVistaList().add(filtroEstado);

            getFilterHelper().setVista(vista1);
            setSelectedViewId(vista1.getIdVista());
        }

    }

    public String filtraPorAlertaPendiente() {
        return filtraPorAlerta(EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta());
    }

    public String filtraPorAlertaPorVencer() {
        return filtraPorAlerta(EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta());
    }

    public String filtraPorAlertaVencido() {
        System.out.println("filtraPorAlertaVencido()");
        return filtraPorAlerta(EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta());
    }

    public String filtraPorAlerta(TipoAlerta alerta) {
        Vista vista1 = applicationBean.getVistaPorAlerta(alerta);
        return applyViewFilter(vista1);
    }

    public String filtraRevisarActualizaciones() {
        Vista vista1 = applicationBean.getVistaRevisarActualizacion();
        return applyViewFilter(vista1);
    }

    public void filterCatCarpetas() {
//        //System.out.println("filterCatCarpetas");
        pagination = null;
        Vista vista1 = new Vista(Caso.class);
        vista1.setIdUsuarioCreadaPor(userSessionBean.getCurrent());
        vista1.setNombre("Casos de la Categoria " + getCategorySelected().getNombre());

        FiltroVista filtroEstado = new FiltroVista();
        filtroEstado.setIdCampo("idEstado");
        filtroEstado.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
        filtroEstado.setValor(EnumEstadoCaso.ABIERTO.getEstado().getIdEstado());
        filtroEstado.setIdVista(vista1);

        vista1.getFiltrosVistaList().add(filtroEstado);

        FiltroVista filtroCategoria = new FiltroVista();
        filtroCategoria.setIdCampo("idCategoria");
        filtroCategoria.setIdComparador(EnumTipoComparacion.EQ.getTipoComparacion());
        filtroCategoria.setValor(getCategorySelected().getIdCategoria().toString());
        filtroCategoria.setIdVista(vista1);

        vista1.getFiltrosVistaList().add(filtroCategoria);

        applyVista(vista1);
    }

    public void checkAllItems() {
        for (Caso caso : (List<Caso>) getItems().getWrappedData()) {
            caso.setSelected(true);
        }
    }

    public void unCheckAllItems() {
        for (Caso caso : (List<Caso>) getItems().getWrappedData()) {
            caso.setSelected(false);
        }
    }

    public void refresh() {
        recreateModel();
    }

    public void tagManyCasosManyTags() {

        int count = 0;
        if (selectedEtiquetas != null && !selectedEtiquetas.isEmpty()) {

            for (Caso caso : getSelectedItems()) {
                try {
                    if (caso.getEtiquetaList() == null) {
                        caso.setEtiquetaList(new LinkedList<Etiqueta>());
                    }
                    List<AuditLog> changeLog = new ArrayList<AuditLog>();
                    changeLog.add(getManagerCasos().createLogReg(caso, "Lista de etiquetas", selectedEtiquetas.toString(), caso.getEtiquetaList().toString()));
                    caso.getEtiquetaList().addAll(selectedEtiquetas);
                    getJpaController().mergeCaso(caso, changeLog);
                    count++;
                } catch (Exception ex) {
                    Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            JsfUtil.addSuccessMessage(count + " casos etiquetados exitosamente.");
        }
    }

    public boolean puedeResponderCasoSeleccionado() {
        if (filtroAcceso.verificarAccesoAFuncionResponderCualquierCaso()) {
            return true;
        }
        if (current.getOwner() != null) {
            Usuario usuarioActual = userSessionBean.getCurrent();
            if (current.getOwner().equals(usuarioActual)) {
                return true;
            }
        }
        return false;
    }

    public boolean puedeModificarCasoSeleccionado() {
        if (filtroAcceso.verificarAccesoAFuncionEditarCualquierCaso()) {
            return true;
        }
        if (current.getOwner() != null) {
            Usuario usuarioActual = userSessionBean.getCurrent();
            if (current.getOwner().equals(usuarioActual)) {
                return true;
            }
        }
        return false;
    }

    public boolean puedeModificarVistaSeleccionada() {
        boolean isOwner = false;

        if (getFilterHelper().getVista().getIdUsuarioCreadaPor() != null) {
            Usuario usuarioActual = userSessionBean.getCurrent();
            if (getFilterHelper().getVista().getIdUsuarioCreadaPor().equals(usuarioActual)) {
                isOwner = true;
            }
        }

        return filtroAcceso.isAdministradorDelSistema()
                || (isOwner && filtroAcceso.verificarAccesoAFuncionAdministrarVistas());
    }

    public boolean verificarTomarCaso() {
        return verificarAsignarCaso();
    }

    public boolean verificarTransferirCaso() {
        return (filtroAcceso.verificarAccesoAFuncionAsignarTransferirCaso() && current.isOpen() && current.hasAnOwner());
    }

    public boolean verificarAsignarCaso() {
        return (filtroAcceso.verificarAccesoAFuncionAsignarTransferirCaso() && current.isOpen() && !current.hasAnOwner());
    }

    public boolean verificarEliminarCaso() {
        return (filtroAcceso.verificarAccesoAFuncionEliminarCaso());
    }

    public boolean verificarReabrirCaso() {
        return (current.isClosed() && puedeModificarCasoSeleccionado());
    }

    public boolean verificarCrearColaborativo() {
        return (current.hasAnOwner() && puedeModificarCasoSeleccionado() && current.isOpen());
    }

    public boolean verificarCrearActividad() {
        return (current.hasAnOwner() && puedeModificarCasoSeleccionado() && current.isOpen());
    }

    public boolean verificarResponderCaso() {
        return (current.hasAnOwner() && current.isOpen() && puedeResponderCasoSeleccionado());
    }

    public boolean verificarGrabarCaso() {
        return (puedeModificarCasoSeleccionado());
    }

    public boolean verificarRelacionarCaso() {
        return current.isOpen() && puedeModificarCasoSeleccionado();
    }

    public boolean verificarCerrarCaso() {
        return current.isOpen() && puedeModificarCasoSeleccionado();
    }

    public String descartarBorrador() {

        //List<AuditLog> changeLog = new ArrayList<AuditLog>();
        try {
            //  current.setFechaModif(Calendar.getInstance().getTime());
            current.setRespuesta(null);
            //getJpaController().mergeCaso(current, changeLog);
            JsfUtil.addSuccessMessage("Borrador descartado");
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, "No se ha descartado el borrador");
        }

        return "/script/caso/Edit";
    }

    public String cerrarCaso() {

        List<AuditLog> changeLog = new ArrayList<AuditLog>();

        try {
            changeLog.add(getManagerCasos().createLogReg(current, "Estado", EnumEstadoCaso.CERRADO.getEstado().getNombre(), current.getIdEstado().getNombre()));
            current.setIdEstado(EnumEstadoCaso.CERRADO.getEstado());
            current.setFechaCierre(Calendar.getInstance().getTime());
            changeLog.add(getManagerCasos().createLogReg(current, "FechaCierre", current.getFechaCierre().toString(), ""));

            JsfUtil.addSuccessMessage(resourceBundle.getString("caso.cerrar.ok"));
            getJpaController().mergeCaso(current, changeLog);

            HelpDeskScheluder.unscheduleCambioAlertas(getJpaController().getSchema(), current.getIdCaso());

            if (current.getCasosHijosList() != null) {
                for (Caso casoHijo : current.getCasosHijosList()) {
                    casoHijo.setIdEstado(EnumEstadoCaso.CERRADO.getEstado());
                    casoHijo.setFechaCierre(Calendar.getInstance().getTime());

                    JsfUtil.addSuccessMessage(resourceBundle.getString("caso.cerrar.ok"));
                    changeLog.add(getManagerCasos().createLogReg(casoHijo, "Estado", "Cerrado", "Abierto"));
                    getJpaController().mergeCaso(casoHijo, changeLog);

                    HelpDeskScheluder.unscheduleCambioAlertas(getJpaController().getSchema(), casoHijo.getIdCaso());

//                    HelpDeskScheluder.unscheduleTask(casoHijo.getIdCaso().toString() + "a", HelpDeskScheluder.GRUPO_CASOS);
//                    HelpDeskScheluder.unscheduleTask(casoHijo.getIdCaso().toString() + "b", HelpDeskScheluder.GRUPO_CASOS);
                }
            }
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "cerrarCaso", e);
            JsfUtil.addErrorMessage(resourceBundle.getString("caso.cerrar.nook"));
        }

        return "/script/caso/Edit";
    }

    public String reabrirCaso() {

        List<AuditLog> changeLog = new ArrayList<AuditLog>();

        try {
            //TODO implementar tipo-> SubEstado selection in reabrir dialog
            changeLog.add(getManagerCasos().createLogReg(current, "Estado", EnumEstadoCaso.ABIERTO.getEstado().getNombre(), current.getIdEstado().getNombre()));
            current.setIdEstado(EnumEstadoCaso.ABIERTO.getEstado());
            current.setFechaModif(Calendar.getInstance().getTime());
            current.setFechaCierre(null);

            getJpaController().mergeCaso(current, changeLog);
            JsfUtil.addSuccessMessage(resourceBundle.getString("caso.abrir.ok"));
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(resourceBundle.getString("caso.abrir.nook"));
        }

        return "/script/caso/Edit";
    }

    public String guardarBorrador() {
        //List<AuditLog> changeLog = new ArrayList<AuditLog>();
        try {
            current.setRespuesta(textoNota);
            //current.setFechaModif(Calendar.getInstance().getTime());
            //changeLog.add(getManagerCasos().createLogReg(current, "Borrador", "Se crea borrador de respuesta", ""));
            //getJpaController().mergeCaso(current, changeLog);
            JsfUtil.addSuccessMessage("Borrador guardado");
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, "No se ha guardado el borrador");
        }

        return "/script/caso/Edit";
    }

    public String agregarRelacionado() {

        try {

            Caso casorel = getJpaController().find(Caso.class, new Long(idCaserel));
            if (new Long(idCaserel).equals(current.getIdCaso())) {
                addErrorMessage("No se puede relacionar el caso a si mismo.");
            }
            if (casorel != null) {
                casorel.getCasosRelacionadosList().add(current);
                getJpaController().mergeCaso(casorel, getManagerCasos().verificaCambios(casorel));
                current.getCasosRelacionadosList().add(casorel);
                getJpaController().mergeCaso(current, getManagerCasos().verificaCambios(current));

//                current = getJpaController().getCasoFindByIdCaso(current.getIdCaso());
                JsfUtil.addSuccessMessage(resourceBundle.getString("casorel.ok"));
                //getManagerCasos().createLogReg(current, "Casos Relacionados", idCaserel, "");
                idCaserel = "";
                refreshCaso();
            } else {
                JsfUtil.addErrorMessage(resourceBundle.getString("casorel.notfound"));
            }
        } catch (Exception e) {
//            idCaserel = "";
//            current = getJpaController().getCasoFindByIdCaso(current.getIdCaso());
            JsfUtil.addErrorMessage(resourceBundle.getString("casorel.nook"));
        }

        return "/script/caso/Edit";
    }

    public void asignarCaso() {
        List<AuditLog> changeLog = new ArrayList<AuditLog>();
        try {
            String motivo = textoNota;

            if (tipoTransferOption == 1) {
                //change agent
                if (usuarioSeleccionadoTransfer != null && !usuarioSeleccionadoTransfer.equals(current.getOwner())) {
                    String idUsuarioNuevo = usuarioSeleccionadoTransfer.getIdUsuario();
                    String idUsuarioAnterior = current.getOwner() == null ? "No asignado" : current.getOwner().getIdUsuario();

                    current.setOwner(usuarioSeleccionadoTransfer);
                    Nota nota = crearObjetoNota(getSelected(), false, motivo, EnumTipoNota.TRANSFERENCIA_CASO.getTipoNota(), false);
                    if (current.getNotaList() == null) {
                        current.setNotaList(new ArrayList<Nota>());
                    }
                    current.getNotaList().add(nota);

                    changeLog.add(getManagerCasos().createLogReg(current, "Agente Asignado", idUsuarioNuevo, idUsuarioAnterior));
                    getJpaController().mergeCaso(current, changeLog);

                    addInfoMessage("El caso se ha tranferido con éxito al agente " + usuarioSeleccionadoTransfer.getIdUsuario());

                    if (ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).isSendNotificationOnTransfer()) {
//                        if (current.getIdArea() != null && current.getIdArea().getIdArea() != null) {
                        try {
                            MailNotifier.notifyCasoAssigned(current, motivo, getUserSessionBean().getTenantId());
                        } catch (Exception ex) {
                            addWarnMessage(ex.getMessage());
                        }
                    }

                    if (applicationBean != null) {
                        if (!userSessionBean.getCurrent().equals(current.getOwner())) {
                            applicationBean.sendFacesMessageNotification(current.getOwner().getIdUsuario(), "El caso #" + current.getIdCaso() + ": " + current.getTema() + ". Ha sido asignado a ud. por " + userSessionBean.getCurrent().getCapitalName() + "!");
                        }
                    }

//                    if (!userSessionBean.getCurrent().equals(current.getOwner())) {
//                        BackendEvent event = new BackendEvent();
//                        event.setEventType(BackendEvent.TYPE_ASSIGNED_CASO);
//                        event.setAssigner(userSessionBean.getCurrent().getIdUsuario());
//                        event.setIdCaso(current.getIdCaso());
//                        event.setIdUsuarioAssigned(current.getOwner().getIdUsuario());
//                        backendEvent.fire(event);
//                    }
                } else {
                    addWarnMessage("El agente seleccionado no puede ser vacío ni igual al agente actual.");
                }
            } else if (tipoTransferOption == 2) {
                //change client
                if (emailClienteSeleccionadoTransfer != null) {
                    String idUsuarioNuevo = emailClienteSeleccionadoTransfer.getEmailCliente();
                    String idUsuarioAnterior = current.getEmailCliente() == null ? "Sin Cliente" : current.getEmailCliente().getEmailCliente();

                    current.setEmailCliente(emailClienteSeleccionadoTransfer);

                    changeLog.add(getManagerCasos().createLogReg(current, "Cliente", idUsuarioNuevo, idUsuarioAnterior));
                    getJpaController().mergeCaso(current, changeLog);

                    JsfUtil.addSuccessMessage("El caso se ha tranferido con éxito al cliente " + emailClienteSeleccionadoTransfer.getCliente().getCapitalName());
                }
            } else {
                //error
                JsfUtil.addErrorMessage("Error!!!");
            }

            recreateModel();

            prepareDynamicCaseData(current);

            RequestContext context = RequestContext.getCurrentInstance();
            context.execute("transferir.hide()");

//            current = getJpaController().getCasoFindByIdCaso(current.getIdCaso());
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
            addErrorMessage("Error al tratar de transferir el caso");
        }

//        return "/script/caso/Edit";
    }

    public String tomarCaso() {

        List<AuditLog> changeLog = new ArrayList<AuditLog>();

        try {
            current = getJpaController().find(Caso.class, current.getIdCaso());
            if (current.getOwner() == null) {
                current.setOwner(userSessionBean.getCurrent());
                current.setRevisarActualizacion(false);
                changeLog.add(getManagerCasos().createLogComment(current, "Agente se autoasigna el caso"));
                getJpaController().mergeCaso(current, changeLog);
                JsfUtil.addSuccessMessage(resourceBundle.getString("tomarcasoOK"));
            } else {
                JsfUtil.addSuccessMessage("El caso ya ha sido tomado por " + current.getOwner().toString());
            }

            if (current.getCasosHijosList() != null) {
                for (Caso casoHijo : current.getCasosHijosList()) {
                    if (casoHijo.getOwner() == null) {
                        casoHijo.setOwner(userSessionBean.getCurrent());
                        casoHijo.setRevisarActualizacion(false);
                        changeLog.add(getManagerCasos().createLogComment(casoHijo, "Agente se autoasigna el caso"));
                        getJpaController().mergeCaso(casoHijo, changeLog);
                    } else {
                        JsfUtil.addSuccessMessage("No se puede asignar el caso N°" + casoHijo.getIdCaso() + ", ya ha sido asignado a " + casoHijo.getOwner().toString());
                    }
                }
            }
        } catch (Exception e) {
//            //System.out.println("tomar caso, exception: " + e.getClass().getName());
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, resourceBundle.getString("tomarcasoNOOK"));
        }

        return "/script/caso/Edit";

    }

    /**
     * Arma una nota (there should be a Nota factory class)
     *
     * @return
     */
    private Nota crearObjetoNota(Caso caso, boolean publica, String texto, TipoNota tipo, boolean customer) {
//        boolean guardarNota = true;
        try {
            Nota nota = new Nota();
            nota.setFechaCreacion(Calendar.getInstance().getTime());
            nota.setIdCaso(caso);
            nota.setTexto(texto);
            nota.setIdCanal(EnumCanal.PORTAL.getCanal());

            nota.setVisible(publica);
            if (customer) {
                nota.setEnviadoPor(userSessionBean.getEmailCliente().getEmailCliente());
                nota.setCreadaPor(null);
                nota.setTipoNota(EnumTipoNota.RESPUESTA_DE_CLIENTE.getTipoNota());
                caso.setRevisarActualizacion(true);
            } else {

                nota.setEnviadoPor(null);
                nota.setCreadaPor(userSessionBean.getCurrent());
                nota.setTipoNota(tipo);

                if (caso.getOwner() != null) {
                    if (!nota.getCreadaPor().getIdUsuario().equals(caso.getOwner().getIdUsuario())) {
                        caso.setRevisarActualizacion(true);
                    }
                }
            }
            return nota;
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage("ha ocurrido un error en armarNota");
            return null;
        }
    }

    public void customerCreateNota() {
        try {
            Nota nota = crearObjetoNota(getSelected(), true, getTextoNota(), null, true);
            if (current.getNotaList() == null) {
                current.setNotaList(new ArrayList<Nota>());
            }
            current.getNotaList().add(nota);
            List<AuditLog> changeLog = new ArrayList<AuditLog>();
            changeLog.add(getManagerCasos().createLogComment(current, "El cliente ha enviado un comentario al caso"));
            getJpaController().mergeCaso(current, changeLog);
            JsfUtil.addSuccessMessage(resourceBundle.getString("NotaCreated"));

        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "customerCreateNota", e);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al crear Nota.",
                    e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
//        return "ticket";
    }

    public void crearNota(boolean notifyClient) {
//        listaActividadesOrdenada = null;
//        tipoNota = EnumTipoNota.NOTA.getTipoNota();
//        this.armarNota(current, textoNotaVisibilidadPublica, textoNota, EnumTipoNota.NOTA.getTipoNota());

        try {
            String correoEnviado = null;
            List<AuditLog> changeLog = new ArrayList<AuditLog>();

            if (notifyClient) {

                Nota nota = crearObjetoNota(getSelected(), textoNotaVisibilidadPublica, getTextoNota(), EnumTipoNota.NOTA.getTipoNota(), false);
                if (current.getNotaList() == null) {
                    current.setNotaList(new ArrayList<Nota>());
                }
                current.getNotaList().add(nota);
                changeLog.add(getManagerCasos().createLogComment(current, "Nuevo Comentario del tipo: " + nota.getTipoNota().getNombre()));

                correoEnviado = MailNotifier.emailClientCasoUpdatedByAgent(current, getUserSessionBean().getTenantId());
                if (correoEnviado != null) {

                    Nota notaNotif = crearObjetoNota(current, true, "Cliente ha sido notificado por email sobre la actualización del caso. <br/>Email: <br/>" + correoEnviado,
                            EnumTipoNota.NOTIFICACION_UPDATE_CASO.getTipoNota(), false);
                    current.getNotaList().add(notaNotif);
                    changeLog.add(getManagerCasos().createLogReg(current, "Notificación al cliente (email)", correoEnviado, ""));

                    addInfoMessage("Notificación enviada al cliente exitósamente.");

                    getJpaController().mergeCaso(current, changeLog);

                    JsfUtil.addSuccessMessage(resourceBundle.getString("NotaCreated"));
                    prepareDynamicCaseData(current);

                    refreshCaso();
                    disableReplyMode();

                } else {
                    changeLog.add(getManagerCasos().createLogComment(current, "Envío de Correo de Respuesta falló"));
                    addErrorMessage("No se puede enviar Notificación al cliente a travéz de Area " + current.getIdArea().getNombre());
                }
            } else {

                Nota nota = crearObjetoNota(getSelected(), textoNotaVisibilidadPublica, getTextoNota(), EnumTipoNota.NOTA.getTipoNota(), false);
                if (current.getNotaList() == null) {
                    current.setNotaList(new ArrayList<Nota>());
                }
                current.getNotaList().add(nota);
                changeLog.add(getManagerCasos().createLogComment(current, "Nuevo Comentario del tipo: " + nota.getTipoNota().getNombre()));
                selectedClipping = null;//TODO: CHECK IF NOTA WAS CREATED FIRST
                textoNota = null;
                adjuntarArchivosARespuesta = false;
                getJpaController().mergeCaso(current, changeLog);

                JsfUtil.addSuccessMessage(resourceBundle.getString("NotaCreated"));
                prepareDynamicCaseData(current);
                refreshCaso();
                disableReplyMode();
            }

        } catch (Exception ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String editNota(boolean notifyClient) {

        try {
            if (notifyClient) {
                String correoEnviado = MailNotifier.emailClientCasoUpdatedByAgent(current, getUserSessionBean().getTenantId());
                if (correoEnviado != null) {
                    Nota notaNotif = crearObjetoNota(current, true, "Cliente ha sido notificado por email sobre la actualización del caso. <br/>Email: <br/>" + correoEnviado,
                            EnumTipoNota.NOTIFICACION_UPDATE_CASO.getTipoNota(), false);
                    current.getNotaList().add(notaNotif);
                    JsfUtil.addSuccessMessage("Notificación enviada al cliente exitósamente.");
                } else {
                    JsfUtil.addSuccessMessage("No se pudo enviar la Notificación por correo.");
                }
            }
            getJpaController().mergeCaso(current, getManagerCasos().verificaCambios(current));
            refreshCaso();
            JsfUtil.addSuccessMessage("El comentario fue actualizado exitósamente.");
            executeInClient("Nota.hide()");

        } catch (Exception ex) {
            JsfUtil.addSuccessMessage("El comentario no pudo ser actualizado.");
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "/script/caso/Edit";
    }

    /**
     * Mobile responder
     *
     * @return
     */
    public String responderMobile() {

        boolean result = createEmailComment();
        if (result) {
            return "pm:actividadesCaso";
        } else {
            return null;
        }

    }

    /**
     * responder actionListener desde web page.
     */
    public void responder() {

        createEmailComment();
        //return "/script/caso/Edit";
    }

    private boolean createEmailComment() {
        List<AuditLog> changeLog = new ArrayList<AuditLog>();
        boolean correoEnviado = false;
        try {
//            progresoEnvioRespuesta = 0;

            String bodyTxt = textoNota.trim();

            if (bodyTxt.isEmpty()) {
                JsfUtil.addErrorMessage("La respuesta no tiene texto, verifíque e intente nuevamente");
//                progresoEnvioRespuesta = 100;
                return false;
            }

            String subject = ManagerCasos.formatIdCaso(current.getIdCaso()) + " " + current.getTema() + " - " + resourceBundle.getString("email.tituloNotas");
            correoEnviado = enviarCorreo(current.getEmailCliente().getEmailCliente(), subject, bodyTxt);

            if (correoEnviado) {
                Nota nota = crearObjetoNota(getSelected(), true, textoNota.trim(), EnumTipoNota.RESPUESTA_A_CLIENTE.getTipoNota(), false);
                if (current.getNotaList() == null) {
                    current.setNotaList(new ArrayList<Nota>());
                }
                current.getNotaList().add(nota);

                changeLog.add(getManagerCasos().createLogComment(current, "Se envía respuesta por email"));
                getJpaController().mergeCaso(current, changeLog);
                prepareDynamicCaseData(current);
                selectedClipping = null;//
                adjuntarArchivosARespuesta = false;
                textoNota = null;
                addInfoMessage("Respuesta enviada exitósamente.");
                disableReplyMode();
            } else {
                changeLog.add(getManagerCasos().createLogComment(current, "Envio de Correo de Respuesta falló"));
                addErrorMessage(resourceBundle.getString("respuestaCasoNOOK"));
            }

        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "createEmailComment", e);
            JsfUtil.addErrorMessage(e, resourceBundle.getString("respuestaCasoNOOK"));
            return false;
        }
//        finally {
//            progresoEnvioRespuesta = 100;
//        }

        return correoEnviado;
    }

    private boolean enviarCorreo(final String emailCliente, final String subject, String mensaje) throws NumberFormatException {
        boolean sended = false;

        List<EmailAttachment> attachments = new ArrayList<EmailAttachment>();

        if (selectedAttachmensForMail != null) {
            System.out.println("selectedAttachmensForMail:" + selectedAttachmensForMail);
            StringBuilder attachmentsNames = new StringBuilder();
//            attachments = new ArrayList<EmailAttachment>(selectedAttachmensForMail.size());
            Iterator iteradorAttachments = selectedAttachmensForMail.iterator();
            int indexAtt = 0;
            while (iteradorAttachments.hasNext()) {
                Long idatt = new Long((String) iteradorAttachments.next());
                Attachment att = getJpaController().find(Attachment.class, idatt);
                Archivo archivo = getJpaController().find(Archivo.class, att.getIdAttachment());
                if (archivo != null) {
                    EmailAttachment emailAttachment = new EmailAttachment();
                    emailAttachment.setData(archivo.getArchivo());
                    emailAttachment.setMimeType(att.getMimeType());
                    emailAttachment.setName(att.getNombreArchivo());
                    emailAttachment.setPath(att.getNombreArchivo());
                    emailAttachment.setSize(archivo.getArchivo().length);
                    attachments.add(emailAttachment);
                    attachmentsNames.append(att.getNombreArchivo()).append("<br/>");
                    indexAtt++;
                } else {

                }
//                progresoEnvioRespuesta = (int) (((50f / (float) selectedAttachmensForMail.size())) * indexAtt);
            }
            if (!(attachmentsNames.toString().isEmpty())) {
                StringBuilder nuevoTextoNota = new StringBuilder(textoNota);
                nuevoTextoNota.append("<br/><b>Archivos adjuntos:</b><br/>");
                nuevoTextoNota.append(attachmentsNames);
                textoNota = nuevoTextoNota.toString();
            }
        }

        if (incluirHistoria) {
            StringBuilder nuevoTextoNota = new StringBuilder(textoNota);
            nuevoTextoNota.append("<br/><b>Se ha agregado la historia del caso</b><br/>");
            textoNota = nuevoTextoNota.toString();

            StringBuilder textoMensaje = new StringBuilder(mensaje);
            textoMensaje.append(obtenerHistorial());
            mensaje = textoMensaje.toString();
            incluirHistoria = false;
        }

        try {
            EmailClient emailClient = MailClientFactory.getInstance(getUserSessionBean().getTenantId(), (current.getIdArea() != null) ? current.getIdArea().getIdArea() : null);
            if (emailClient != null) {

                emailClient.sendHTML(emailCliente, subject, mensaje, attachments);
                sended = true;

            } else {
                addErrorMessage("El envío de correo no está habilitado desde el Área " + current.getIdArea().getNombre() + ", favor comunicarse con el administrador de su cuenta para que configure la cuenta de correo.");
                sended = false;
            }

            if (sended) {
                addInfoMessage("Correo enviado exitósamente.");
            }
        } catch (EmailException ex) {
            sended = false;
            addErrorMessage("No se pudo enviar el correo: " + ex.getMessage());
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "sendHTML failed", ex);
        }

        return sended;
    }

    public String upload() {
        if (uploadFile != null) {
            FacesMessage msg = new FacesMessage("Succesful", uploadFile.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
        return null;
    }

    public void deleteAttachment(ActionEvent actionEvent) {

        try {
            getJpaController().remove(Archivo.class, new Long(idFileDelete));
        } catch (Exception e) {
            //ignore.
        }
        try {
            String filename = null;
//            Attachment att = getJpaController().find(Attachment.class, new Long(idFileDelete));
//            String nombre = att.getNombreArchivo();
//            getJpaController().remove(Attachment.class, new Long(idFileDelete));
//            getSelected().getAttachmentList().remove(att);
            for (Attachment attachment : getSelected().getAttachmentList()) {
                if (attachment.getIdAttachment().equals(new Long(idFileDelete))) {
                    filename = attachment.getNombreArchivo();
                    getSelected().getAttachmentList().remove(attachment);
                    getJpaController().merge(getSelected());
                    break;
                }
            }
//            refreshCaso();
            JsfUtil.addSuccessMessage("Archivo " + idFileDelete + " borrado exitósamente.");
            getJpaController().persist(getManagerCasos().createLogReg(current, "Archivo borrado", filename, ""));

        } catch (Exception e) {
            JsfUtil.addErrorMessage("No se ha podido borrar el archivo");
            Log.createLogger(this.getClass().getName()).logInfo("No se ha podido borrar el archivo");
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            //e.printStackTrace();
        }
    }

    public void removeAttachment(Long idAtt) {
        try {
            if (current.getAttachmentList() != null) {
                for (Attachment attachment : current.getAttachmentList()) {
                    if (attachment.getIdAttachment().equals(idAtt)) {
                        current.getAttachmentList().remove(attachment);
                        JsfUtil.addSuccessMessage("Archivo " + attachment.getNombreArchivo() + " removido de la lista.");
                    }
                }
            }

        } catch (Exception e) {
            JsfUtil.addSuccessMessage("Ocurrio un error no esperado, No se ha podido remover el archivo.");
            Log.createLogger(this.getClass().getName()).logInfo("No se ha podido borrar el archivo");
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            //e.printStackTrace();
        }
    }

    private String obtenerHistorial() {
        StringBuilder sbuilder = new StringBuilder("<br/><hr/><b>HISTORIA DEL CASO</b><br/>");
        prepareDynamicCaseData(current);
        Collection<Nota> notas = listaActividadesOrdenada;
        if (notas != null) {
            for (Nota nota : notas) {
                if (nota.getTipoNota().equals(EnumTipoNota.RESPUESTA_A_CLIENTE.getTipoNota()) || nota.getTipoNota().equals(EnumTipoNota.RESPUESTA_DE_CLIENTE.getTipoNota())) {
                    sbuilder.append(creaMensajeOriginal(current, nota));
                }
            }
        }

        sbuilder.append(creaMensajeOriginal(current));
        return sbuilder.toString();
    }

    private String creaMensajeOriginal(Caso caso) {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("<br/>");
        sbuilder.append("INICIO MENSAJE ORIGINAL");
        sbuilder.append("<br/>");
        sbuilder.append("Email cliente: ");
        sbuilder.append(caso.getEmailCliente());
        sbuilder.append("<br/>");
        sbuilder.append("Asunto: ");
        sbuilder.append(caso.getTema());
        sbuilder.append("<br/>");
        sbuilder.append("Fecha: ");
        sbuilder.append(caso.getFechaCreacion());
        sbuilder.append("<br/>");
        sbuilder.append("Mensaje: ");
        sbuilder.append("<br/>");
        sbuilder.append(replaceEmbeddedImages(caso.getDescripcion()));
        sbuilder.append("<br/>");
        sbuilder.append("FIN MENSAJE ORIGINAL");
        sbuilder.append("<hr/>");
        return sbuilder.toString();
    }

    private String creaMensajeOriginal(Caso caso, Nota nota) {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("INICIO ");
        sbuilder.append(nota.getTipoNota().getNombre().toUpperCase());
        sbuilder.append("<br/>");
        sbuilder.append("email cliente: ");
        sbuilder.append(caso.getEmailCliente());
        sbuilder.append("<br/>");
        sbuilder.append("Fecha: ");
        sbuilder.append(nota.getFechaCreacion());
        sbuilder.append("<br/>");
        sbuilder.append("Mensaje: ");
        sbuilder.append("<br/>");
        sbuilder.append(replaceEmbeddedImages(nota.getTexto()));
        sbuilder.append("<br/>");
        sbuilder.append("FIN ");
        sbuilder.append(nota.getTipoNota().getNombre().toUpperCase());
        sbuilder.append("<hr/>");
        return sbuilder.toString();
    }

    public String deleteAttachment() {
        try {
            Attachment att = getJpaController().find(Attachment.class, new Long(idFileDelete));
            String nombre = att.getNombreArchivo();
            Archivo archivo = getJpaController().find(Archivo.class, att.getIdAttachment());
            if (archivo != null) {
                getJpaController().remove(Archivo.class, archivo.getIdAttachment());
            }
            getJpaController().remove(Attachment.class, att.getIdAttachment());

//            att = new Attachment();
//            att.setIdCaso(current);
//            Collection col = getJpaController().findListAttachmentEntities(att);
//            current.setAttachmentList(col);
            current = getJpaController().find(Caso.class, current.getIdCaso());
            JsfUtil.addSuccessMessage("Archivo " + nombre + " borrado");
            getJpaController().persist(getManagerCasos().createLogReg(current, "Archivo borrado", nombre, ""));

        } catch (Exception e) {
            JsfUtil.addSuccessMessage("No se ha podido borrar el archivo");
            Log.createLogger(this.getClass().getName()).logInfo("No se ha podido borrar el archivo");
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            //e.printStackTrace();
        }

        return "/script/caso/Edit";
    }

    /**
     * Envia el archivo al componente de PrimeFace filoDownload
     *
     * @param id Identificador de Attachment
     * @param modo Modo de download, 0=inmediato, 1=pregunta a usuario
     * @return
     */
    public StreamedContent bajarArchivo(Long id) {
        try {

            Archivo archivo = getJpaController().find(Archivo.class, id);

            if (archivo != null) {
                return new DefaultStreamedContent(new ByteArrayInputStream(archivo.getArchivo()), archivo.getContentType(), archivo.getFileName());
            } else {
                JsfUtil.addErrorMessage("Error, El archivo con id " + id + " no existe!!");
                return new DefaultStreamedContent();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(resourceBundle.getString("attachment.error"));
            return new DefaultStreamedContent();
        }
    }

    /**
     * @deprecated @param document
     */
    public void preProcessPDF(Object document) throws IOException, BadElementException, DocumentException {

        Document pdf = (Document) document;
        pdf.open();
        pdf.addAuthor("www.itcs.cl");
//        pdf.addHeader("Header1", "header2");
        pdf.addTitle("Itcs, Integrated Solutions.");
        pdf.setMargins(1, 1, 1, 1);
        pdf.setPageSize(PageSize.A4);

//        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
//        String logo = servletContext.getRealPath("") + File.separator + "resources" + File.separator + "images" + File.separator + "headerForm.png";
//
//        pdf.add(Image.getInstance(logo));
    }

    /**
     * @deprecated @param document
     */
    public void postProcessXLS(Object document) {
        HSSFWorkbook wb = (HSSFWorkbook) document;
        HSSFSheet sheet = wb.getSheetAt(0);

        HSSFRow header = sheet.getRow(0);

        HSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setFillForegroundColor(HSSFColor.BRIGHT_GREEN.index);
        cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        for (int i = 0; i < header.getPhysicalNumberOfCells(); i++) {
            HSSFCell cell = header.getCell(i);
            cell.setCellStyle(cellStyle);
            sheet.autoSizeColumn((short) i);
        }
    }

    /**
     *
     * @return
     */
    public StreamedContent exportAllItems() {

        try {
            List<Caso> casos = getJpaController().findCasoEntities(getFilterHelper().getVista(), userSessionBean.getCurrent(), (getDefaultOrderBy()));//all
            OutputStream output = new ByteArrayOutputStream();
            //Se crea el libro Excel
            WritableWorkbook workbook
                    = Workbook.createWorkbook(output);

            //Se crea una nueva hoja dentro del libro
            WritableSheet sheet
                    = workbook.createSheet("Casos " + getFilterHelper().getVista().toString(), 0);
            WritableCellFormat timesBoldUnderline;
            WritableFont times10ptBoldUnderline = new WritableFont(WritableFont.TIMES, 10, WritableFont.BOLD, false, UnderlineStyle.SINGLE);
            timesBoldUnderline = new WritableCellFormat(times10ptBoldUnderline);
            // Lets automatically wrap the cells
            timesBoldUnderline.setWrap(true);
            DateFormat customDateFormat = new DateFormat("d/m/yy h:mm");
            WritableCellFormat dateFormat = new WritableCellFormat(customDateFormat);

            String[] titulos = new String[]{"Fecha Creación", "ID Caso", "Rut", "Nombre", "Apellido", "Telefono", "Telefono adicional", "Ciudad",
                "Comentarios", "Tema", "Email", "Propietario", "Estado", "Subestado", "SLA", "Fecha de cierre"};

            int col = 0;
            if (titulos != null) {
                for (String string : titulos) {
                    sheet.addCell(new Label(col, 0, string, timesBoldUnderline));
                    col++;
                }
            }

            int row = 1;
            if (casos != null) {
                for (Caso caso : casos) {
                    sheet.addCell(new DateTime(0, row, caso.getFechaCreacion(), dateFormat));
                    sheet.addCell(new Label(1, row, caso.getIdCaso() + ""));
                    if (caso.getEmailCliente() != null && caso.getEmailCliente().getCliente() != null) {
                        sheet.addCell(new Label(2, row, caso.getEmailCliente().getCliente().getRut()));
                        sheet.addCell(new Label(3, row, caso.getEmailCliente().getCliente().getNombres()));
                        sheet.addCell(new Label(4, row, caso.getEmailCliente().getCliente().getApellidos()));
                        sheet.addCell(new Label(5, row, caso.getEmailCliente().getCliente().getFono1()));
                        sheet.addCell(new Label(6, row, caso.getEmailCliente().getCliente().getFono2()));
                        sheet.addCell(new Label(7, row, caso.getEmailCliente().getCliente().getDirParticular()));
                        sheet.addCell(new Label(10, row, caso.getEmailCliente().getEmailCliente()));
                    }
                    sheet.addCell(new Label(8, row, caso.getDescripcionTxt()));
                    sheet.addCell(new Label(9, row, caso.getTema()));
                    if (caso.getOwner() != null) {
                        sheet.addCell(new Label(11, row, caso.getOwner().getCapitalName()));
                    }
                    if (caso.getIdEstado() != null) {
                        sheet.addCell(new Label(12, row, caso.getIdEstado().getNombre()));
                    }
                    if (caso.getIdSubEstado() != null) {
                        sheet.addCell(new Label(13, row, caso.getIdSubEstado().getNombre()));
                    }
                    if (caso.getNextResponseDue() != null) {
                        sheet.addCell(new DateTime(14, row, caso.getNextResponseDue(), dateFormat));
                    }
                    if (caso.getFechaCierre() != null) {
                        sheet.addCell(new DateTime(15, row, caso.getFechaCierre(), dateFormat));
                    }
//                for (int i = 0; i <= 15; i++) {
//                    System.out.print(sheet.getCell(i, row).getContents()+";");                    
//                }
//                //System.out.println("");
                    row++;
                }
            }

            workbook.write();
            workbook.close();
            InputStream decodedInput = new ByteArrayInputStream(((ByteArrayOutputStream) output).toByteArray());
            ////System.out.println("Ejemplo finalizado.");
            return new DefaultStreamedContent(
                    decodedInput, "application/vnd.ms-excel", "casos.xls");
        } catch (Exception ex) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage("Ocurrió un problema al generar el Documento Excel. Favor contacte a soporte de sistema.");
            return new DefaultStreamedContent();

        }

    }

    public void subir(FileUploadEvent event) {
        try {
            String nombre = event.getFile().getFileName();
            nombre = nombre.substring(nombre.lastIndexOf(File.separator) + 1);
            if (nombre.lastIndexOf('\\') > 0) {
                nombre = nombre.substring(nombre.lastIndexOf('\\') + 1);
            }
            InputStream is = event.getFile().getInputstream();
            long size = event.getFile().getSize();
            byte[] bytearray = new byte[(int) size];
            is.read(bytearray);

            if (this.current != null) {
                getManagerCasos().crearAdjunto(bytearray, null, current, nombre, event.getFile().getContentType(), size);
                getManagerCasos().mergeCaso(current, getManagerCasos().createLogReg(current, "Archivo attachado", event.getFile().getFileName(), ""));
//                prepareAttachmentInfo(current);
                JsfUtil.addSuccessMessage("Archivo " + nombre + " subido con exito");
            } else {
                JsfUtil.addErrorMessage("Archivo " + nombre + " no se puede cargar. Favor intente nuevamente.");
            }
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, resourceBundle.getString("attachment.upload.error"));
        }
    }

    public void uploadToSaveLater(FileUploadEvent event) {
        try {
            String nombre = event.getFile().getFileName();
            nombre = nombre.substring(nombre.lastIndexOf(File.separator) + 1);
            if (nombre.lastIndexOf('\\') > 0) {
                nombre = nombre.substring(nombre.lastIndexOf('\\') + 1);
            }
            InputStream is = event.getFile().getInputstream();
            long size = event.getFile().getSize();
            byte[] bytearray = new byte[(int) size];
            is.read(bytearray);

            Random randomGenerator = new Random();
            long n = randomGenerator.nextLong();
            if (n > 0) {
                n = n * (-1);
            }

            Archivo archivo = new Archivo(n);
            archivo.setArchivo(bytearray);

            if (current.getAttachmentList() == null) {
                current.setAttachmentList(new ArrayList<Attachment>());
            }

            Attachment attach = new Attachment(n);
            attach.setIdCaso(current);
            attach.setNombreArchivo(nombre);
            attach.setFileSize(size);
            attach.setMimeType(event.getFile().getContentType());
            attach.setArchivo(archivo);
            current.getAttachmentList().add(attach);

            //getJpaController().persistAttachment(attach);//later
            //archivo.setIdAttachment(attach.getIdAttachment());run later in create
            //getJpaController().persistArchivo(archivo); // run later in create
            JsfUtil.addSuccessMessage("Archivo " + nombre + " subido éxitosamente.");
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "Error al subir archivo", e);
            JsfUtil.addErrorMessage(e, e.getLocalizedMessage());
        }
    }

    public String mergeCliente() {
        try {
            getJpaController().merge(current.getEmailCliente().getCliente());
            JsfUtil.addSuccessMessage("Datos cliente actualizados exitosamente.");

            if (userSessionBean.isValidatedCustomerSession()) {
                return null;
            } else if (userSessionBean.isValidatedSession()) {
                return "/script/caso/Edit";
            } else {
                return null;
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, e.getMessage());
            return null;
        }
    }

    public String update() {
        try {
            update(current);
            return "/script/caso/Edit";
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, resourceBundle.getString("PersistenceErrorOccured"), e);
            JsfUtil.addErrorMessage(e, e.getMessage());
            return null;
        }

    }

//    public void parseCotizacionEmail() {
//        try {
//
//            Action parserAction = new ParseCotizacionZoomInmobiliarioAction(getJpaController());
//            parserAction.execute(current);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
    public void updateListener() {
        update();
    }

    public void update(Caso casoToUpdate) throws IllegalOrphanException, NonexistentEntityException, RollbackFailureException, Exception {

        if ((null != casoToUpdate.getEmailCliente().getCliente().getRut()) && (!casoToUpdate.getEmailCliente().getCliente().getRut().isEmpty())) {
            if (!UtilesRut.validar(casoToUpdate.getEmailCliente().getCliente().getRut())) {
                JsfUtil.addErrorMessage("Rut invalido");
                return;
            }
        }
//////            if (casoToUpdate.getOwner() != null) {
//////                Usuario actualUser = userSessionBean.getCurrent();
//////                if (!casoToUpdate.getOwner().equals(actualUser)) {
//////                    //RequestContext.getCurrentInstance().push(current.getOwner().getIdUsuario(), "Se ha actualizado el caso: "+current.getIdCaso());
//////                    ////System.out.println("\n\n\npushed!!\n\n\n");
//////                }
//////            }

        casoToUpdate.setFechaModif(Calendar.getInstance().getTime());
        List<AuditLog> lista = getManagerCasos().verificaCambios(casoToUpdate);
        getManagerCasos().mergeCaso(casoToUpdate, lista);
        JsfUtil.addSuccessMessage(resourceBundle.getString("CasoUpdated"));

//        if (applicationBean != null) {
//            if (!userSessionBean.getCurrent().equals(casoToUpdate.getOwner())) {
//                applicationBean.sendFacesMessageNotification(casoToUpdate.getOwner().getIdUsuario(), "Los datos del caso #" + casoToUpdate.getIdCaso() + ": " + casoToUpdate.getTema() + ". Ha sido modificado por " + userSessionBean.getCurrent().getCapitalName() + ", Favor revisar!");
//            }
//        }
//        if (!userSessionBean.getCurrent().equals(casoToUpdate.getOwner())) {
//            BackendEvent event = new BackendEvent();
//            event.setEventType(BackendEvent.TYPE_EDITED_CASO);
//            event.setAssigner(userSessionBean.getCurrent().getIdUsuario());
//            event.setIdCaso(casoToUpdate.getIdCaso());
//            event.setIdUsuarioAssigned(casoToUpdate.getOwner().getIdUsuario());
//            backendEvent.fire(event);
//        }
    }

    public String updateDescripcion() {
        String salida = update();
//        if (salida != null) {
//            getManagerCasos().createLogReg("Descripcion", "Descripcion actualizada", "");
//        }
        return salida;
    }

    public String destroy() {
        if (current == null) {
            return null;
        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        if (performDestroy(current)) {
            JsfUtil.addSuccessMessage("El caso #" + current.getIdCaso() + " fue eliminado exitósamente.");
            try {
                HelpDeskScheluder.unscheduleCambioAlertas(getJpaController().getSchema(), current.getIdCaso());
            } catch (SchedulerException ex) {
                Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "unscheduleTask", ex);
            }
            actualizaArbolDeCategoria();
            current = null;
            pagination = null;
            recreateModel();
            return "inbox";
        } else {
            addErrorMessage("El caso #" + current.getIdCaso() + " no se pudo eliminar.");
            return null;
        }
    }

    public String destroySelected() {
        try {
//            List<Caso> notDeleted = new LinkedList<Caso>();

            if (getSelectedItems() != null) {
                int countDeleted = 0;
                if (getSelectedItems().size() <= 0) {
                    JsfUtil.addErrorMessage("Debe seleccionar al menos un caso.");
                } else {
                    if (getSelectedItems() != null) {
                        for (Caso ticket : getSelectedItems()) {
                            ticket.setSelected(false);
                            if (performDestroy(ticket)) {
                                countDeleted++;
                            } else {
//                                notDeleted.add(ticket);
                                JsfUtil.addErrorMessage("El caso #" + ticket.getIdCaso() + " no se pudo eliminar.");
                            }
                        }
                    }

                    JsfUtil.addSuccessMessage(countDeleted + " Casos fueron eliminados exitósamente.");
                }
//                selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
//                unCheckAllItems();
                recreateModel();

            }

        } catch (Exception e) {
            addErrorMessage(e.getLocalizedMessage());
        }

        return "index?faces-redirect=true";
//        return "inbox";
        //return "List";
    }

    private boolean performDestroy(Caso caso) {
        try {

//            caso = getJpaController().getCasoFindByIdCaso(caso.getIdCaso());
//            if (caso.getNotaList() != null) {
//                for (Nota nota : caso.getNotaList()) {
//                    getJpaController().removeNota(nota);
//                }
//            }
//            if (caso.getAttachmentList() != null) {
//                for (Attachment attachment : caso.getAttachmentList()) {
//                    Archivo archivo = getJpaController().getArchivoFindByIdAttachment(attachment.getIdAttachment());
//                    if (archivo != null) {
//                        getJpaController().removeArchivo(archivo);
//                    }
////                    getJpaController().removeAttachment(attachment);//replaced by @CascadeOnDelete
//                }
//            }
            getJpaController().persist(getManagerCasos().createLogComment(caso, "Se elimina el caso manualmente"));
//            getJpaController().removeCaso(caso);
            getJpaController().remove(Caso.class, caso.getIdCaso());

            return true;

        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "Error al tratar de eliminar caso " + caso.toString() + ":" + e.getMessage(), e);
            return false;
        }
    }

    private void prepareNotas(Caso selectedCaso) {
        try {

            if (userSessionBean.isValidatedCustomerSession()) {
                listaActividadesOrdenada = getJpaController().getNotasPublicasFindByIdCaso(selectedCaso.getIdCaso());
            } else if (userSessionBean.isValidatedSession()) {
                listaActividadesOrdenada = getJpaController().getNotaFindByIdCaso(selectedCaso.getIdCaso());
            } else {
                listaActividadesOrdenada = Collections.EMPTY_LIST;
            }

            Collections.sort(listaActividadesOrdenada);

        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "ERROR", e);
            listaActividadesOrdenada = null;
        }
    }

    public List<CasoCustomField> getCurrentCasoCustomFieldList() {

//        System.out.println("getCurrentCasoCustomFieldList...");
//        System.out.println("getTipoCaso:" + current.getTipoCaso());
//        System.out.println("CustomFieldList:" + current.getTipoCaso().getCustomFieldList());
        if (current.getCasoCustomFieldList() == null) {
            current.setCasoCustomFieldList(new ArrayList<CasoCustomField>());
        }

        try {
            List<CasoCustomField> removeCasoCustomFieldList = new LinkedList<CasoCustomField>();
            if (current != null && current.getTipoCaso() != null && current.getTipoCaso().getCustomFieldList() != null) {
                //remove all old
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "CasoController.getCurrentCasoCustomFieldList:{0}", current.getTipoCaso().getCustomFieldList());
                for (CasoCustomField casoCustomField : current.getCasoCustomFieldList()) {
                    if (!casoCustomField.getCustomField().getTipoCasoList().contains(current.getTipoCaso())) {
                        removeCasoCustomFieldList.add(casoCustomField);
                    }
                }
                current.getCasoCustomFieldList().removeAll(removeCasoCustomFieldList);
                //add all new
                for (CustomField customField : current.getTipoCaso().getCustomFieldList()) {
                    CasoCustomField casoCustomField = new CasoCustomField(customField.getCustomFieldPK().getFieldKey(), customField.getCustomFieldPK().getEntity(), current);
                    if (!current.getCasoCustomFieldList().contains(casoCustomField)) {
                        casoCustomField.setCustomField(customField);
                        current.getCasoCustomFieldList().add(casoCustomField);
                        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "added:{0}", casoCustomField);
//                        merge = true;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "error at getItemsSubEstadoCasoAvailableSelectOneCasoAbierto", ex);
        }

        System.out.println("return:" + current.getCasoCustomFieldList());
        return current.getCasoCustomFieldList();
    }

//    private void prepareCustomFields() {
//        System.out.println("prepareCustomFields...");
////        boolean merge = false;
//        try {
//            final List<CustomField> casoCustomFields = getJpaController().getCustomFieldsForCaso();
//            System.out.println("casoCustomFields:" + casoCustomFields);
//
//            if (current.getCasoCustomFieldList() == null) {
//                current.setCasoCustomFieldList(new ArrayList<CasoCustomField>());
//            }
//
//            if (casoCustomFields != null && !casoCustomFields.isEmpty()) {
//                //there are custom fields so we need to set a value
//                for (CustomField customField : casoCustomFields) {
//                    CasoCustomField casoCustomField = new CasoCustomField(customField.getCustomFieldPK().getFieldKey(), customField.getCustomFieldPK().getEntity(), current);
//                    if (!current.getCasoCustomFieldList().contains(casoCustomField)) {
//                        casoCustomField.setCustomField(customField);
//                        current.getCasoCustomFieldList().add(casoCustomField);
//                        System.out.println("added:" + casoCustomField);
////                        merge = true;
//                    }
//                }
//            }
////            if(merge){
////                List<AuditLog> changeLog = new ArrayList<AuditLog>();
////                changeLog.add(getManagerCasos().createLogReg(current, "se agregan custom fields al caso.", "", ""));
////                getJpaController().mergeCaso(current, changeLog);
////            }
//        } catch (Exception e) {
//            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "ERROR", e);
//        }
//    }
    public void prepareDynamicCaseData(Caso selectedCaso) {
        try {
            prepareNotas(selectedCaso);
            disableReplyMode();
//            prepareCustomFields();
//            prepareAttachmentInfo(selectedCaso);
//            adjuntarArchivosARespuesta = false;
//            this.activeIndexWestPanelForms = 0;
            setActiveIndexdescOrComment(0);
//            this.activeIndexWestPanel = 0;
        } catch (Exception ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "prepareDynamicCaseData", ex);
        }
    }

//    private void prepareAttachmentInfo(Caso selectedCaso) {
//        cantidadAttachmentsEmbedded = getJpaController().countAttachmentsWContentId(selectedCaso).intValue();
//        attachmentWOContentId = new LinkedList<Attachment>();
//        for (Attachment attachment : selectedCaso.getAttachmentList()) {
//            if (attachment.getContentId() == null) {
//                attachmentWOContentId.add(attachment);
//            }
//        }
////        attachmentWOContentId = getJpaController().findAttachmentsWOContentId(current);
//    }
//    public List<Nota> getNotasCustomerVisibleList() {
//        try {
////            if (listaActividadesOrdenada == null) {
////                //System.out.println("se crea listaActividadesOrdenada");
//            listaActividadesOrdenada = getJpaController().getNotasPublicasFindByIdCaso(getSelected());
//            Collections.sort(listaActividadesOrdenada);
////            }
//            return listaActividadesOrdenada;
//        } catch (Exception e) {
//            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "", e);
//            return new LinkedList();
//        }
//    }
    public int getSelectedItemsCount() {
        try {
            return getSelectedItems().size();
        } catch (Exception e) {
            //ignore.
        }
        return 0;
    }



    public List<String> getSelectedAttachmensForMail() {
        return selectedAttachmensForMail;
    }

    public void setSelectedAttachmensForMail(List<String> selectedAttachmensForMail) {
        this.selectedAttachmensForMail = selectedAttachmensForMail;
    }

    public TreeNode getCategoria() {
        return categoria;
    }

    public void setCategoria(TreeNode categoria) {
        if (categoria != null) {
            categorySelected = (Categoria) categoria.getData();
        }
        this.categoria = categoria;
    }

//    public SelectItem[] getItemsAvailableSelectMany() {
//        return JsfUtil.getSelectItems(getJpaController().getCasoFindAll(), false);
//    }
//
//    public SelectItem[] getItemsAvailableSelectOne() {
//        return JsfUtil.getSelectItems(getJpaController().getCasoFindAll(), true);
//    }
//    public SelectItem[] getClippingsItemsAvailableSelectOne() {
//        return JsfUtil.getSelectItems(getJpaController().findAll(Clipping.class), true);
//    }
    public void handleClippingSelectChangeEvent() {
        textoNota = "";
        if (selectedClipping != null && selectedClipping.getTexto() != null) {
            textoNota = ClippingsPlaceHolders.buildFinalText(selectedClipping.getTexto(), current, getUserSessionBean().getTenantId());
        } else {
            System.out.println("No se ha seleccionado ningun clipping.");
        }

    }

//    public SelectItem[] getItemsPageSizeAvailable() {
//        return JsfUtil.getSelectItems(getPagination().getPageSizesAvailable(), false);
//    }
    public SelectItem[] getItemsSubEstadoCasoAvailableSelectOneCasoCerrado() {
        List<SubEstadoCaso> lista = new ArrayList<SubEstadoCaso>();
        try {
            if (current.getTipoCaso() != null) {
                final List<SubEstadoCaso> subEstadoCasofindByIdEstadoAndTipoCaso = getJpaController().getSubEstadoCasofindByIdEstadoAndTipoCaso(EnumEstadoCaso.CERRADO.getEstado(), current.getTipoCaso());
                if (subEstadoCasofindByIdEstadoAndTipoCaso != null) {
                    for (SubEstadoCaso subEstadoCaso : subEstadoCasofindByIdEstadoAndTipoCaso) {
                        if (!lista.contains(subEstadoCaso)) {
                            lista.add(subEstadoCaso);
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JsfUtil.getSelectItems(lista, false);
    }

    public SelectItem[] getItemsSubEstadoCasoAvailableSelectOneCasoAbierto() {
        List<SubEstadoCaso> lista = new ArrayList<SubEstadoCaso>();

        try {
            if (current.getTipoCaso() != null) {
                final List<SubEstadoCaso> subEstadoCasofindByIdEstadoAndTipoCaso = getJpaController().getSubEstadoCasofindByIdEstadoAndTipoCaso(EnumEstadoCaso.ABIERTO.getEstado(), current.getTipoCaso());
                if (subEstadoCasofindByIdEstadoAndTipoCaso != null) {
                    for (SubEstadoCaso subEstadoCaso : subEstadoCasofindByIdEstadoAndTipoCaso) {
                        if (!lista.contains(subEstadoCaso)) {
                            lista.add(subEstadoCaso);
                        }
                    }
                }

            }
        } catch (Exception ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "error at getItemsSubEstadoCasoAvailableSelectOneCasoAbierto", ex);
        }

        return JsfUtil.getSelectItems(lista, false);
    }

//    public SelectItem[] getItemsPrioridadAvailableSelectOneOpen() {
//        try {
//            if (current.getTipoCaso() != null) {
//                return JsfUtil.getSelectItems(getJpaController().findPrioridadByTipoCaso(current.getTipoCaso()), false);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
    /**
     * @return the uploadFile
     */
    public UploadedFile getUploadFile() {
        return uploadFile;
    }

    /**
     * @param uploadFile the uploadFile to set
     */
    public void setUploadFile(UploadedFile uploadFile) {
        this.uploadFile = uploadFile;
    }

    /**
     * @return the cantidadDeRespuestasACliente
     */
    public int getCantidadDeRespuestasACliente() {
        return cantidadDeRespuestasACliente;
    }

    /**
     * @return the cantidadDeRespuestasAlCliente
     */
    public int getCantidadDeRespuestasDelCliente() {
        return cantidadDeRespuestasDelCliente;
    }

    /**
     * @return the emailCliente_wizard
     */
    public String getEmailCliente_wizard() {
        return emailCliente_wizard;
    }

    /**
     * @param emailCliente_wizard the emailCliente_wizard to set
     */
    public void setEmailCliente_wizard(String emailCliente_wizard) {
        this.emailCliente_wizard = emailCliente_wizard;
    }

    private boolean compararStrings(String header, String value) {
        boolean resp = header.equals(value);
        ////System.out.println("comparando "+header+" vs "+value+" resp="+resp);
        return resp;
    }

    /**
     * @return the emailCliente_wizard_existeEmail
     */
    public boolean isEmailCliente_wizard_existeEmail() {
        return emailCliente_wizard_existeEmail;
    }

    /**
     * @param emailCliente_wizard_existeEmail the
     * emailCliente_wizard_existeEmail to set
     */
    public void setEmailCliente_wizard_existeEmail(boolean emailCliente_wizard_existeEmail) {
        this.emailCliente_wizard_existeEmail = emailCliente_wizard_existeEmail;
    }

    /**
     * @return the emailCliente_wizard_existeCliente
     */
    public boolean isEmailCliente_wizard_existeCliente() {
        return emailCliente_wizard_existeCliente;
    }

    /**
     * @param emailCliente_wizard_existeCliente the
     * emailCliente_wizard_existeCliente to set
     */
    public void setEmailCliente_wizard_existeCliente(boolean emailCliente_wizard_existeCliente) {
        this.emailCliente_wizard_existeCliente = emailCliente_wizard_existeCliente;
    }

    /**
     * @return the htmlToView
     */
    public String getHtmlToView() {
        return htmlToView;
    }

    /**
     * @param htmlToView the htmlToView to set
     */
    public void setHtmlToView(String htmlToView) {
        this.htmlToView = htmlToView;
    }

    private String createEmbeddedImage(String contentId) {
        try {
            Attachment att = getJpaController().findByContentId('<' + contentId + '>', current);
            Archivo archivo = getJpaController().find(Archivo.class, att.getIdAttachment());
            if (archivo != null) {
                String base64Image = Base64.encodeBase64String(archivo.getArchivo());
                return base64Image;
            }
        } catch (Exception ex) {
            Log.createLogger(this.getClass().getName()).log(Level.WARNING, "no se encuentra el archivo: " + contentId, ex);
        }
        return null;
    }

    public boolean isNotaCliente(Nota nota) {
        try {
            if (EnumTipoNota.RESPUESTA_DE_CLIENTE.getTipoNota().equals(nota.getTipoNota())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean isVisibleByCliente(Nota nota) {
        try {
            if (EnumTipoNota.RESPUESTA_A_CLIENTE.getTipoNota().equals(nota.getTipoNota())
                    || EnumTipoNota.RESPUESTA_DE_CLIENTE.getTipoNota().equals(nota.getTipoNota())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param userSessionBean the userSessionBean to set
     */
    public void setUserSessionBean(UserSessionBean userSessionBean) {
        this.userSessionBean = userSessionBean;
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

    /**
     * @return the categorySelected
     */
    public Categoria getCategorySelected() {
        return categorySelected;
    }

    /**
     * @return the selectedClipping
     */
    public Clipping getSelectedClipping() {
        return selectedClipping;
    }

    /**
     * @param selectedClipping the selectedClipping to set
     */
    public void setSelectedClipping(Clipping selectedClipping) {
        this.selectedClipping = selectedClipping;
    }

    /**
     * @return the emailCliente_wizard_updateCliente
     */
    public boolean isEmailCliente_wizard_updateCliente() {
        return emailCliente_wizard_updateCliente;
    }

    /**
     * @param emailCliente_wizard_updateCliente the
     * emailCliente_wizard_updateCliente to set
     */
    public void setEmailCliente_wizard_updateCliente(boolean emailCliente_wizard_updateCliente) {
        this.emailCliente_wizard_updateCliente = emailCliente_wizard_updateCliente;
    }

    /**
     * @return the rutCliente_wizard
     */
    public String getRutCliente_wizard() {
        return rutCliente_wizard;
    }

    /**
     * @param rutCliente_wizard the rutCliente_wizard to set
     */
    public void setRutCliente_wizard(String rutCliente_wizard) {
        this.rutCliente_wizard = rutCliente_wizard;
    }

    /**
     * @return the textoNotaVisibilidadPublica
     */
    public boolean isTextoNotaVisibilidadPublica() {
        return textoNotaVisibilidadPublica;
    }

    /**
     * @param textoNotaVisibilidadPublica the textoNotaVisibilidadPublica to set
     */
    public void setTextoNotaVisibilidadPublica(boolean textoNotaVisibilidadPublica) {
        this.textoNotaVisibilidadPublica = textoNotaVisibilidadPublica;
    }

    /**
     * @return the swatch
     */
    public String getSwatch() {
        return swatch;
    }

    /**
     * @param swatch the swatch to set
     */
    public void setSwatch(String swatch) {
        this.swatch = swatch;
    }

    /**
     * @return the selectedNota
     */
    public Nota getSelectedNota() {
        return selectedNota;
    }

    /**
     * @param selectedNota the selectedNota to set
     */
    public void setSelectedNota(Nota selectedNota) {
        this.selectedNota = selectedNota;
    }

    /**
     * @return the tipoTransferOption
     */
    public Integer getTipoTransferOption() {
        return tipoTransferOption;
    }

    /**
     * @param tipoTransferOption the tipoTransferOption to set
     */
    public void setTipoTransferOption(Integer tipoTransferOption) {
        this.tipoTransferOption = tipoTransferOption;
    }

    /**
     * @return the usuarioSeleccionadoTransfer
     */
    public Usuario getUsuarioSeleccionadoTransfer() {
        return usuarioSeleccionadoTransfer;
    }

    /**
     * @param usuarioSeleccionadoTransfer the usuarioSeleccionadoTransfer to set
     */
    public void setUsuarioSeleccionadoTransfer(Usuario usuarioSeleccionadoTransfer) {
        this.usuarioSeleccionadoTransfer = usuarioSeleccionadoTransfer;
    }

    /**
     * @return the emailClienteSeleccionadoTransfer
     */
    public EmailCliente getEmailClienteSeleccionadoTransfer() {
        return emailClienteSeleccionadoTransfer;
    }

    /**
     * @param emailClienteSeleccionadoTransfer the
     * emailClienteSeleccionadoTransfer to set
     */
    public void setEmailClienteSeleccionadoTransfer(EmailCliente emailClienteSeleccionadoTransfer) {
        this.emailClienteSeleccionadoTransfer = emailClienteSeleccionadoTransfer;
    }

    /**
     * @param idFileRemove the idFileRemove to set
     */
    public void setIdFileRemove(Long idFileRemove) {
        this.idFileRemove = idFileRemove;
    }

    /**
     * @return the selectedEtiquetas
     */
    public List<Etiqueta> getSelectedEtiquetas() {
        return selectedEtiquetas;
    }

    /**
     * @param selectedEtiquetas the selectedEtiquetas to set
     */
    public void setSelectedEtiquetas(List<Etiqueta> selectedEtiquetas) {
        this.selectedEtiquetas = selectedEtiquetas;
    }

    /**
     * @param vistaController the vistaController to set
     */
    public void setVistaController(VistaController vistaController) {
        this.vistaController = vistaController;
    }

    /**
     * @return the justCreadedNotaId
     */
    public Integer getJustCreadedNotaId() {
        return justCreadedNotaId;
    }

    /**
     * @param justCreadedNotaId the justCreadedNotaId to set
     */
    public void setJustCreadedNotaId(Integer justCreadedNotaId) {
        this.justCreadedNotaId = justCreadedNotaId;
    }

    /**
     * @param applicationBean the applicationBean to set
     */
    public void setApplicationBean(ApplicationBean applicationBean) {
        this.applicationBean = applicationBean;
    }

//    /**
//     * @return the activeIndexMenuAccordionPanel
//     */
//    public int getActiveIndexMenuAccordionPanel() {
//        return activeIndexMenuAccordionPanel;
//    }
//
//    /**
//     * @param activeIndexMenuAccordionPanel the activeIndexMenuAccordionPanel to
//     * set
//     */
//    public void setActiveIndexMenuAccordionPanel(int activeIndexMenuAccordionPanel) {
//        this.activeIndexMenuAccordionPanel = activeIndexMenuAccordionPanel;
//    }
    /**
     * @return the activeIndexWestPanel
     */
    public int getActiveIndexWestPanel() {
        return activeIndexWestPanel;
    }

    /**
     * @param activeIndexWestPanel the activeIndexWestPanel to set
     */
    public void setActiveIndexWestPanel(int activeIndexWestPanel) {
        this.activeIndexWestPanel = activeIndexWestPanel;
    }

    /**
     * @return the activeIndexWestPanelForms
     */
    public int getActiveIndexWestPanelForms() {
        return activeIndexWestPanelForms;
    }

    /**
     * @param activeIndexWestPanelForms the activeIndexWestPanelForms to set
     */
    public void setActiveIndexWestPanelForms(int activeIndexWestPanelForms) {
        this.activeIndexWestPanelForms = activeIndexWestPanelForms;
    }

    /**
     * @return the selectedViewId
     */
    public Integer getSelectedViewId() {
        return selectedViewId;
    }

    /**
     * @param selectedViewId the selectedViewId to set
     */
    public void setSelectedViewId(Integer selectedViewId) {
        this.selectedViewId = selectedViewId;
    }

    /**
     * @return the stepNewCasoIndex
     */
    public int getStepNewCasoIndex() {
        return stepNewCasoIndex;
    }

    /**
     * @param stepNewCasoIndex the stepNewCasoIndex to set
     */
    public void setStepNewCasoIndex(int stepNewCasoIndex) {
        this.stepNewCasoIndex = stepNewCasoIndex;
    }

    /**
     * @return the listaActividadesOrdenada
     */
    public List<Nota> getListaActividadesOrdenada() {
        return listaActividadesOrdenada;
    }

    /**
     * @param listaActividadesOrdenada the listaActividadesOrdenada to set
     */
    public void setListaActividadesOrdenada(List<Nota> listaActividadesOrdenada) {
        this.listaActividadesOrdenada = listaActividadesOrdenada;
    }

    /**
     * @return the selectedItemIndex
     */
    public int getSelectedItemIndex() {
        return selectedItemIndex;
    }

    /**
     * @return the adjuntarArchivosARespuesta
     */
    public boolean isAdjuntarArchivosARespuesta() {
        return adjuntarArchivosARespuesta;
    }

    /**
     * @param adjuntarArchivosARespuesta the adjuntarArchivosARespuesta to set
     */
    public void setAdjuntarArchivosARespuesta(boolean adjuntarArchivosARespuesta) {
        this.adjuntarArchivosARespuesta = adjuntarArchivosARespuesta;
    }

    /**
     * @return the activeIndexdescOrComment
     */
    public int getActiveIndexdescOrComment() {
        return activeIndexdescOrComment;
    }

    /**
     * @param activeIndexdescOrComment the activeIndexdescOrComment to set
     */
    public void setActiveIndexdescOrComment(int activeIndexdescOrComment) {
        this.activeIndexdescOrComment = activeIndexdescOrComment;
    }

    /**
     * @return the reglaTriggerSelected
     */
    public ReglaTrigger getReglaTriggerSelected() {
        return reglaTriggerSelected;
    }

    /**
     * @param reglaTriggerSelected the reglaTriggerSelected to set
     */
    public void setReglaTriggerSelected(ReglaTrigger reglaTriggerSelected) {
        this.reglaTriggerSelected = reglaTriggerSelected;
    }

    /**
     * @return the accionToRunSelected
     */
    public String getAccionToRunSelected() {
        return accionToRunSelected;
    }

    /**
     * @param accionToRunSelected the accionToRunSelected to set
     */
    public void setAccionToRunSelected(String accionToRunSelected) {
        this.accionToRunSelected = accionToRunSelected;
    }

    /**
     * @return the accionToRunParametros
     */
    public String getAccionToRunParametros() {
        return accionToRunParametros;
    }

    /**
     * @param accionToRunParametros the accionToRunParametros to set
     */
    public void setAccionToRunParametros(String accionToRunParametros) {
        this.accionToRunParametros = accionToRunParametros;
    }

    /**
     * @return the replyMode
     */
    public boolean isReplyMode() {
        return replyMode;
    }

    /**
     * @param replyMode the replyMode to set
     */
    public void setReplyMode(boolean replyMode) {
        this.replyMode = replyMode;
    }

    /**
     * @return the filterViewToggle
     */
    public boolean isFilterViewToggle() {
        return filterViewToggle;
    }

    /**
     * @param filterViewToggle the filterViewToggle to set
     */
    public void setFilterViewToggle(boolean filterViewToggle) {
        this.filterViewToggle = filterViewToggle;
    }

    /**
     * @param applicationBean the applicationBean to set
     */
//    public void setApplicationBean(ApplicationBean applicationBean) {
//        this.applicationBean = applicationBean;
//    }
    @FacesConverter(forClass = Caso.class)
    public static class CasoControllerConverter implements Converter, Serializable {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CasoController controller = (CasoController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "casoController");
            return controller.getJpaController().find(Caso.class, getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
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
            if (object instanceof Caso) {
                Caso o = (Caso) object;
                return getStringKey(o.getIdCaso());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + CasoController.class.getName());
            }
        }
    }
}

class CasoDataModel extends ListDataModel<Caso> implements SelectableDataModel<Caso>, Serializable {

    public CasoDataModel() {
        //nothing
    }

    public CasoDataModel(List<Caso> data) {
        super(data);
    }

    @Override
    public Caso getRowData(String rowKey) {
        List<Caso> listOfCaso = (List<Caso>) getWrappedData();

        if (listOfCaso != null) {
            for (Caso obj : listOfCaso) {
                if (obj.getIdCaso().toString().equals(rowKey)) {
                    return obj;
                }
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(Caso classname) {
        return classname.getIdCaso();
    }
}
