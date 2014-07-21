package com.itcs.helpdesk.util;

import com.itcs.helpdesk.quartz.HelpDeskScheluder;
import com.itcs.commons.email.EmailClient;
import com.itcs.commons.email.EmailMessage;
import com.itcs.helpdesk.jsfcontrollers.CasoController;
import com.itcs.helpdesk.jsfcontrollers.InputValidationBean;
import com.itcs.helpdesk.jsfcontrollers.UsuarioController;
import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.jsfcontrollers.util.UserSessionBean;
import com.itcs.helpdesk.persistence.entities.Archivo;
import com.itcs.helpdesk.persistence.entities.Area;
import com.itcs.helpdesk.persistence.entities.Attachment;
import com.itcs.helpdesk.persistence.entities.AuditLog;
import com.itcs.helpdesk.persistence.entities.Canal;
import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.CasoCustomField;
import com.itcs.helpdesk.persistence.entities.Cliente;
import com.itcs.helpdesk.persistence.entities.CustomField;
import com.itcs.helpdesk.persistence.entities.EmailCliente;
import com.itcs.helpdesk.persistence.entities.ModeloProducto;
import com.itcs.helpdesk.persistence.entities.ModeloProductoPK;
import com.itcs.helpdesk.persistence.entities.Nota;
import com.itcs.helpdesk.persistence.entities.Prioridad;
import com.itcs.helpdesk.persistence.entities.Producto;
import com.itcs.helpdesk.persistence.entities.SubEstadoCaso;
import com.itcs.helpdesk.persistence.entities.TipoCaso;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.persistence.entityenums.EnumAreas;
import com.itcs.helpdesk.persistence.entityenums.EnumCanal;
import com.itcs.helpdesk.persistence.entityenums.EnumEstadoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumPrioridad;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAlerta;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoNota;
import com.itcs.helpdesk.persistence.entityenums.EnumUsuariosBase;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import com.itcs.helpdesk.quartz.TicketAlertStateChangeJob;
import com.itcs.helpdesk.webservices.DatosCaso;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.NoResultException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailAttachment;
import org.quartz.SchedulerException;

/**
 *
 * @author jorge
 */
public class ManagerCasos implements Serializable {

    public static final String PATTERN_ID_CASO = "(\\[go-ref#)(\\d*)(\\])";
    public static final String FORMAT_ID_CASO = "[go-ref#%d]";

    public static final Pattern patternIdCaso = Pattern.compile(PATTERN_ID_CASO, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static final float PORCENTAJE_POR_VENCER = 0.6f;
    private transient JPAServiceFacade jpaController;
//    private final ResourceBundle resourceBundle;
//    private long currentTask = 0;
    private EmailClient emailClient;

    public static String removeAccents(String text) {
        return text == null ? null : Normalizer.normalize(text, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public ManagerCasos() {
//        this.resourceBundle = ResourceBundle.getBundle("Bundle");
    }

    public ManagerCasos(JPAServiceFacade jpaController) {
        this.jpaController = jpaController;
//        this.resourceBundle = ResourceBundle.getBundle("Bundle");
    }

    public void setJpaController(JPAServiceFacade jpaController) {
        this.jpaController = jpaController;
    }

    private JPAServiceFacade getJpaController() {
        return jpaController;
    }

    public void mergeCaso(Caso caso, AuditLog audit) throws Exception {
        List<AuditLog> changeLog = new ArrayList<AuditLog>(1);
        changeLog.add(audit);
        mergeCaso(caso, changeLog);
    }

    public void mergeCaso(Caso caso, List<AuditLog> changeLog) throws Exception {
        getJpaController().mergeCaso(caso, changeLog);
        if (changeLog != null) {
            for (AuditLog auditLog : changeLog) {
                if (auditLog.getCampo().equalsIgnoreCase("nextResponseDue")) {
                    if (caso.getNextResponseDue().after(Calendar.getInstance().getTime())) {
                        caso.setEstadoAlerta(EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta());
                        getJpaController().mergeCaso(caso, verificaCambios(caso));
//                        agendarAlertas(caso);
                        HelpDeskScheluder.scheduleAlertaPorVencer(getJpaController().getSchema(), caso.getIdCaso(), calculaCuandoPasaAPorVencer(caso));

                    } else {
                        caso.setEstadoAlerta(EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta());
                        getJpaController().mergeCaso(caso, verificaCambios(caso));
//                        desagendarCambioAlerta(caso);
                        TicketAlertStateChangeJob.unschedule(
                                TicketAlertStateChangeJob.formatJobId(getJpaController().getSchema(), caso.getIdCaso(),
                                        EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta().getIdalerta()));
                    }
                }
            }
        }
//        createCasoPreventaIfNeeded(caso);//TODO delete automatic creation of preventa
    }

//    public void evaluarEstadoDelCaso(Caso caso) {
//        try {
//            if (caso.getNextResponseDue().after(Calendar.getInstance().getTime())) {
//                caso.setEstadoAlerta(EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta());
//                getJpaController().mergeCaso(caso, verificaCambios(caso));
//                agendarAlertas(caso);
//
//            } else {
//                caso.setEstadoAlerta(EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta());
//                getJpaController().mergeCaso(caso, verificaCambios(caso));
//                desagendarCambioAlerta(caso);
//            }
//        } catch (Exception ex) {
//            Log.createLogger(ManagerCasos.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//    }
    public static void calcularSLA(Caso caso) throws SchedulerException {
        Prioridad prioridad = caso.getIdPrioridad();
        if (prioridad != null) {
            int horas = prioridad.getSlaHoras();
            Calendar calendario = Calendar.getInstance();
            calendario.setTime(caso.getFechaCreacion());
            calendario.add(Calendar.HOUR, horas);
            caso.setNextResponseDue(calendario.getTime());

        }
    }

    /**
     * This method should be marked to parametrize.
     *
     * @param subject
     * @return
     */
    private Long extractIdCaso(String subject) {
        try {
            Matcher m = patternIdCaso.matcher(subject);
            if (m.find()) {
                String id = m.group(2);
                Long idCaso = Long.parseLong(id);
                return idCaso;
            }
        } catch (NumberFormatException e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "NumberFormatException extractIdCaso failed on " + subject, e);
        }
        return null;//means new caso
    }

    public static String formatIdCaso(Long idCaso) {
        return String.format(FORMAT_ID_CASO, new Object[]{idCaso});
    }

    /**
     * Crear un registro en la tabla de log
     *
     * @param valorCampo Nombre del campo
     * @param newValue Nuevo valor
     * @param oldValue Valor antiguo
     */
    public void createLogReg(String valorCampo,
            String newValue, String oldValue, Caso casoActual) {
        try {
            AuditLog audit = new AuditLog();
            audit.setIdUser(EnumUsuariosBase.SISTEMA.getUsuario().getIdUsuario());
            audit.setFecha(Calendar.getInstance().getTime());
            audit.setTabla("Caso");
            audit.setCampo(valorCampo);
            audit.setNewValue(newValue);
            audit.setOldValue(oldValue);
            audit.setIdCaso(casoActual.getIdCaso());
            getJpaController().persist(audit);
        } catch (Exception ex) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "createLogReg", ex);
        }
    }

    private Cliente creaNuevoCliente(DatosCaso datos) throws Exception {
        Cliente persistent = null;
        if (datos.getRut() != null && !StringUtils.isEmpty(datos.getRut())) {
            persistent = getJpaController().findClienteByRut(UtilesRut.formatear(datos.getRut()));
        }

        if (persistent == null) {
            Cliente new_cliente_record = new Cliente();
            new_cliente_record.setNombres(datos.getNombre());
            new_cliente_record.setApellidos(datos.getApellidos());

            if (datos.getRut() != null && !StringUtils.isEmpty(datos.getRut())) {
                new_cliente_record.setRut(UtilesRut.formatear(datos.getRut()));
            } else {
                new_cliente_record.setRut(null);
            }
            new_cliente_record.setFono1(datos.getTelefono());
            new_cliente_record.setFono2(datos.getTelefono2());
            new_cliente_record.setDirParticular(datos.getComuna());
            getJpaController().persist(new_cliente_record);
            persistent = new_cliente_record;
        }
        return persistent;

    }

    private boolean complyWithMinimalDataRequirements(DatosCaso datos) {
        boolean valid = false;
        if (!StringUtils.isEmpty(datos.getEmail())) {
            if (InputValidationBean.isValidEmail(datos.getEmail().toLowerCase().trim())) {
                valid = true;
            }
        }
        return valid;
    }

    public Caso crearCaso(DatosCaso datos, Canal canal) throws Exception {

        Caso createdCaso = null;

        if (complyWithMinimalDataRequirements(datos)) {
            Caso caso = new Caso();

            String address = datos.getEmail().toLowerCase().trim();

            if (address != null) {
                EmailCliente email_cliente = getJpaController().find(EmailCliente.class, address);

                if (email_cliente != null) {
                    if (email_cliente.getCliente() == null) {
                        Cliente cliente_record = creaNuevoCliente(datos);
                        email_cliente.setCliente(cliente_record);
                        getJpaController().persist(email_cliente);
                    } else {

                        if (datos.getTelefono() != null && !datos.getTelefono().isEmpty()) {
                            email_cliente.getCliente().setFono1(datos.getTelefono());
                        }

                        getJpaController().merge(email_cliente.getCliente());
                    }
                    caso.setEmailCliente(email_cliente);
                } else {
                    EmailCliente new_email_cliente = new EmailCliente(address);
                    Cliente cliente_record = creaNuevoCliente(datos);
                    new_email_cliente.setCliente(cliente_record);
                    getJpaController().persist(new_email_cliente);
                    caso.setEmailCliente(new_email_cliente);
                }
            }

            caso.setIdCanal(canal);

            //Tema/Asunto:
            if (StringUtils.isEmpty(datos.getAsunto())) {
                int endIndex = (datos.getDescripcion() != null && !StringUtils.isEmpty(datos.getDescripcion())) ? datos.getDescripcion().length() : 0;
                endIndex = (endIndex < 30) ? endIndex : 30;
                caso.setTema(datos.getDescripcion().substring(0, endIndex));
            } else {
                caso.setTema(datos.getAsunto());
            }

            caso.setDescripcion(datos.getDescripcion());

            //Tipo de caso:
            if (!StringUtils.isEmpty(datos.getTipoCaso())) {
                try {
                    TipoCaso tipoCaso = getJpaController().find(TipoCaso.class, ManagerCasos.removeAccents(datos.getTipoCaso().toLowerCase()));
                    if (tipoCaso != null) {
                        caso.setTipoCaso(tipoCaso);
                    } else {
                        caso.setTipoCaso(EnumTipoCaso.CONTACTO.getTipoCaso());
                        Log.createLogger(this.getClass().getName()).logWarning("No se ha encontrado el tipo de caso: \"" + datos.getTipoCaso() + "\"");
                    }

                } catch (NoResultException ex) {
                    Log.createLogger(this.getClass().getName()).logInfo("No se ha encontrado el tipo de caso: " + datos.getTipoCaso());
                    caso.setTipoCaso(EnumTipoCaso.CONTACTO.getTipoCaso());
//                    caso.setIdSubEstado(EnumSubEstadoCaso.CONTACTO_NUEVO.getSubEstado());
                }
            } else {
                caso.setTipoCaso(EnumTipoCaso.CONTACTO.getTipoCaso());
//                caso.setIdSubEstado(EnumSubEstadoCaso.CONTACTO_NUEVO.getSubEstado());
            }

            //Prioridad:
            if (!StringUtils.isEmpty(datos.getIdPrioridad())) {
                final Prioridad findPrioridad = getJpaController().find(Prioridad.class, datos.getIdPrioridad());
                if (findPrioridad != null) {
                    caso.setIdPrioridad(findPrioridad);
                } else {
                    caso.setIdPrioridad(EnumPrioridad.MEDIA.getPrioridad());
                }
            } else {
                caso.setIdPrioridad(EnumPrioridad.MEDIA.getPrioridad());
            }

            caso.setEstadoAlerta(EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta());

            //Producto:
            if (!StringUtils.isEmpty(datos.getProducto())) {

                try {
                    Producto prod = getJpaController().find(Producto.class, datos.getProducto().trim());

                    if (prod != null) {
                        caso.setIdProducto(prod);

                        //Modelo:
                        if (datos.getModelo() != null) {
                            try {
                                //Primera opcion buscar el modelo directo en la BBDD
                                final ModeloProducto modeloP = getJpaController().find(ModeloProducto.class, new ModeloProductoPK(datos.getModelo(), prod.getIdProducto()));//findModeloProductoById(datos.getModelo(), prod.getIdProducto());
                                if (modeloP != null) {
                                    caso.setIdModelo(modeloP);
                                } else {
                                    //Sino se encuentra buscar por codigo en la lista
                                    if (prod.getModeloProductoList() != null) {
                                        for (ModeloProducto modeloProducto : prod.getModeloProductoList()) {
                                            if (modeloProducto.getIdModelo().equalsIgnoreCase(datos.getModelo().trim())) {
                                                caso.setIdModelo(modeloProducto);
                                            }
                                        }
                                        //Si aun no lo encuentro entonces buscar por nombre en la lista
                                        if (caso.getIdModelo() == null) {
                                            for (ModeloProducto modeloProducto : prod.getModeloProductoList()) {
                                                if (modeloProducto.getNombre().equalsIgnoreCase(datos.getModelo().trim())) {
                                                    caso.setIdModelo(modeloProducto);
                                                }
                                            }
                                        }
                                    }

                                }

                            } catch (Exception ex) {
                                Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "Error al identificar el Modelo " + datos.getModelo() + "Para el producto " + prod, ex);
                            }
                        } else {
                            //logWarning: MODELO NOT SPECIFIED
                            Log.createLogger(this.getClass().getName()).logWarning("No se ha especificado un modelo para el caso.");
                        }
                    } else {
                        Log.createLogger(this.getClass().getName()).logWarning("Producto desconocido: " + datos.getProducto());
                    }

                } catch (Exception ex) {
                    Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "Error al identificar el Producto", ex);
                }
            } else {
                Log.createLogger(this.getClass().getName()).logWarning("No se ha especificado un producto");
            }

            // caso.setIdArea(area);//This is important to know what area/email will received the ticket.
            //I decided area must be calculated based on RULES.
            //but area can never be null!!
            //Area:
            if (!StringUtils.isEmpty(datos.getIdArea())) {
                Area a = getJpaController().find(Area.class, datos.getIdArea());
                caso.setIdArea(a);
            } else {
                caso.setIdArea(EnumAreas.DEFAULT_AREA.getArea());
            }

            if (datos.getTags() != null) {
                caso.setEtiquetaStringList(datos.getTags());
            }

            persistCaso(caso, createLogReg(caso, "Crear", "se crea caso desde " + canal.getNombre(), ""));
//            createLogReg("Crear", "se crea caso desde canal " + caso.getIdCanal().getNombre(), "", caso);
            createdCaso = caso;

            setCustomFieldValuesIfAny(datos, createdCaso);

//            createCasoPreventaIfNeeded(createdCaso);
//            getJpaController().merge(createdCaso);
        } else {
            throw new Exception("No se puede crear el caso. El caso no cumple con los requisitos minimos!");
        }

        return createdCaso;
    }

    /**
     * TODO This code should be replaced by a custom on CREATE Action
     *
     * @param casoOrigen
     */
    private void createCasoPreventaIfNeeded(Caso casoOrigen) {

        if (casoOrigen.getEmailCliente() == null || casoOrigen.getIdProducto() == null) {
            return;//dont do a shit.
        }

        //TODO: This should be a RULE!
        if (!(casoOrigen.getTipoCaso().equals(EnumTipoCaso.COTIZACION.getTipoCaso()) || casoOrigen.getTipoCaso().equals(EnumTipoCaso.CONTACTO.getTipoCaso()))) {
            return;
        }

        try {
            //Si un cliente esta cotizando, hay que crear implicitamente un caso de preVenta para que sea gestionado por el vendedor
            //La Cotizacion queda atachada al caso de preventa, si llegan nuevas cotizaciones se atachan al caso de preventa abierto para ese cliente.
            //select caso from caso where caso.idcliente = cliente and estado = abierto and tipo = preventa
            //se agrega filtro por producto!
            Caso padre = getJpaController().findCasoBy(casoOrigen.getEmailCliente().getEmailCliente(), casoOrigen.getIdProducto(),
                    EnumEstadoCaso.ABIERTO.getEstado(), EnumTipoCaso.PREVENTA.getTipoCaso());
            if (padre == null) {
//                DatosCaso preVentaDatos = new DatosCaso();
                padre = new Caso();
                padre.setTema("Pre-Venta generada para gestión de cliente ");
                padre.setDescripcion("Pre-Venta única generada para gestion del cliente. Este caso contiene sub-casos.");
                padre.setEmailCliente(casoOrigen.getEmailCliente());

                padre.setIdProducto(casoOrigen.getIdProducto());
                padre.setIdModelo(casoOrigen.getIdModelo());
                padre.setEstadoAlerta(casoOrigen.getEstadoAlerta());
                padre.setIdCategoria(casoOrigen.getIdCategoria());
                padre.setTipoCaso(EnumTipoCaso.PREVENTA.getTipoCaso());
                padre.setIdArea(casoOrigen.getIdArea());
                padre.setIdPrioridad(casoOrigen.getIdPrioridad());
                padre.setEtiquetaList(casoOrigen.getEtiquetaList());
                padre.setIdCanal(casoOrigen.getIdCanal());

//                ArrayList<CasoCustomField> casoCustomFields = new ArrayList<CasoCustomField>();
//                for (CasoCustomField origenCF : casoOrigen.getCasoCustomFieldList()) {
//                    CasoCustomField cf = new CasoCustomField(origenCF.getFieldKey(), origenCF.getEntity(), preventaDelProducto);
//                    cf.setValor(origenCF.getValor());
//                    cf.setValor2(origenCF.getValor2());
//                    casoCustomFields.add(cf);
//                }
//                preventaDelProducto.setCasoCustomFieldList(casoCustomFields);
                persistCaso(padre, createLogReg(padre, "PREVENTA", "Se crea caso para manejo de cliente, origen:" + casoOrigen, ""));
            } else {
                padre.setIdProducto(casoOrigen.getIdProducto());
                padre.setIdModelo(casoOrigen.getIdModelo());
                padre.setIdComponente(casoOrigen.getIdComponente());
                padre.setIdSubComponente(casoOrigen.getIdSubComponente());
                padre.setIdCategoria(casoOrigen.getIdCategoria());
                padre.setIdPrioridad(casoOrigen.getIdPrioridad());
            }

//            if (padre != null) {
            casoOrigen.setIdCasoPadre(padre);
            List<Caso> casosHijosList = padre.getCasosHijosList();
            if (casosHijosList == null) {
                casosHijosList = new ArrayList<Caso>();
            }
            casosHijosList.add(casoOrigen);
            padre.setCasosHijosList(casosHijosList);

            getJpaController().merge(padre);
            getJpaController().merge(casoOrigen);
//            }

//            return padre;
            //si no existe se debe crear
            //caso preventa. asociar caso (this)
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "method createCasoPreventaIfNeeded", ex);
//            return null;
        }
    }

    public boolean crearCasoDesdeEmail(Area area, EmailMessage item) throws Exception {
        boolean retorno = false;
//        try {
        String subject = HtmlUtils.extractText(item.getSubject());// item.getSubject();
        String desc = (item.getText());
        if ((subject != null && subject.length() > 100)) {
            //subject is too big
            desc = "Asunto original: " + item.getSubject() + "<br/><br/>" + desc;
            subject = subject.substring(0, 99);
        }
        Long idCaso = extractIdCaso(subject);

        if (idCaso != null) {
            Caso caso = getJpaController().find(Caso.class, idCaso);
            if (caso != null) {
                return crearNotaDesdeEmail(caso, item);
            } else {
                throw new IllegalAccessException("EMAIL REFERENCES A NON-EXISTENT CASE ID:" + idCaso);
            }
        } else {
            DatosCaso datos = new DatosCaso();
            datos.setEmail(item.getFromEmail().toLowerCase().trim());
            final String fromName = item.getFromName();

            if (fromName != null) {
                if (!fromName.contains("ISO-8859-1") && !fromName.contains("UTF-8")) {//Bug Thunderbird
                    String[] nombres = fromName.split(" ");
                    if (nombres.length > 0) {
                        datos.setNombre(nombres[0]);
                    }
                    if (nombres.length > 1) {
                        datos.setApellidos(nombres[1]);
                    }
                } else {
                    Log.createLogger(this.getClass().getName()).logSevere("EMAIL SUBJECT ISSUE! " + fromName);
                }

            }

            datos.setTipoCaso(EnumTipoCaso.CONTACTO.getTipoCaso().getIdTipoCaso());
            datos.setAsunto(subject);
            datos.setDescripcion(desc);
            datos.setIdArea(area.getIdArea());//This is important to know what email received the ticket

            Caso newTicket = crearCaso(datos, EnumCanal.CORREO.getCanal());
            retorno = true;

            handleEmailAttachments(item, newTicket);

        }
//        } catch (Exception ex) {
//            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
//        }
        return retorno;
    }

    public void persistCaso(Caso caso, AuditLog audit) throws Exception {
        List<AuditLog> changeLog = new ArrayList<AuditLog>(1);
        changeLog.add(audit);

        caso.setIdEstado(EnumEstadoCaso.ABIERTO.getEstado());
        caso.setFechaCreacion(Calendar.getInstance().getTime());
        caso.setFechaModif(caso.getFechaCreacion());
        caso.setIdCategoria(null);
        caso.setRevisarActualizacion(true);

        if (caso.getTipoCaso() == null) {
            caso.setTipoCaso(EnumTipoCaso.CONTACTO.getTipoCaso());
        }

        if (caso.getTipoCaso() != null) {
            final TipoCaso tipoCaso = getJpaController().find(TipoCaso.class, caso.getTipoCaso().getIdTipoCaso());
            if (tipoCaso.getSubEstadoCasoList() != null) {
                SubEstadoCaso suposedFirstSubEstadoCaso = null;
                for (SubEstadoCaso subEstadoCaso : tipoCaso.getSubEstadoCasoList()) {
                    if (subEstadoCaso.isFirst()) {
                        caso.setIdSubEstado(subEstadoCaso);
                    }
                    final String toLowerCase = subEstadoCaso.getIdSubEstado().toLowerCase();

                    if (toLowerCase.contains("nuevo") || toLowerCase.contains("new")) {
                        suposedFirstSubEstadoCaso = subEstadoCaso;
                    }
                }
                //Aggggrrrrrrrrrrr sometimes shit happens
                if (suposedFirstSubEstadoCaso != null && caso.getIdSubEstado() == null) {
                    caso.setIdSubEstado(suposedFirstSubEstadoCaso);
                }
            }

        } else {
            throw new Exception("ERROR!! Tipo caso cannot be null");
        }

        caso.setDescripcionTxt(HtmlUtils.stripInvalidMarkup(caso.getDescripcion()));

        ManagerCasos.calcularSLA(caso);

        prepareCustomFields(caso);

        getJpaController().persistCaso(caso, changeLog);

//        if (!caso.getTipoCaso().equals(EnumTipoCaso.PREVENTA.getTipoCaso())) {
        //Cuando este activo acuse de recibo.
        if (caso.getIdArea() != null) {
            if (caso.getIdArea().getEmailAcusederecibo() != null && caso.getIdArea().getEmailAcusederecibo()) {
                MailNotifier.emailClientCasoReceived(caso, getJpaController().getSchema());
//                enviarRespuestaAutomatica(caso, caso.getEmailCliente().getEmailCliente());
            }
        }
//        }

        HelpDeskScheluder.scheduleAlertaPorVencer(getJpaController().getSchema(), caso.getIdCaso(), calculaCuandoPasaAPorVencer(caso));

//        agendarAlertas(caso);
    }

    public Date calculaCuandoPasaAPorVencer(Caso casoFinal) {
        //Calcula tiempo para pasar de pendiente a por vencer
        Calendar calendario = Calendar.getInstance();
        calendario.setTime(casoFinal.getFechaCreacion());
        long resta = casoFinal.getNextResponseDue().getTime() - casoFinal.getFechaCreacion().getTime();
        resta = resta / 60000L;
        int tiempoEnMin = (int) (((float) resta) * PORCENTAJE_POR_VENCER);
        calendario.add(Calendar.MINUTE, tiempoEnMin);
        return calendario.getTime();
    }

    private void setCustomFieldValuesIfAny(DatosCaso datos, Caso current_caso) {
        if (datos == null || current_caso == null || datos.getCustomFields() == null || current_caso.getCasoCustomFieldList() == null) {
            return;
        }
        try {

            for (DatosCaso.CustomField customField : datos.getCustomFields()) {
                for (CasoCustomField casoCustomField : current_caso.getCasoCustomFieldList()) {
                    if (casoCustomField.getFieldKey().equalsIgnoreCase(customField.getFieldKey())) {
                        casoCustomField.setValor(customField.getFieldValue());
                    }
                }
            }

        } catch (Exception ex) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "setCustomFieldValuesIfAny", ex);
        }
    }

    private void prepareCustomFields(Caso selectedCaso) {
        if(selectedCaso == null){
            throw new IllegalArgumentException("ManagerCasos.prepareCustomFields: el caso no puede ser null.");
        }
//        final List<CustomField> casoCustomFields = getJpaController().getCustomFieldsForCaso();
//
//        if (selectedCaso.getCasoCustomFieldList() == null || selectedCaso.getCasoCustomFieldList().isEmpty()) {
//            //values do not exists yet, we must create them empty.
//            List<CasoCustomField> ccfs = new ArrayList<CasoCustomField>();
//            if (casoCustomFields != null) {
//                for (CustomField customField : casoCustomFields) {
//                    CasoCustomField newCasoCustomField = new CasoCustomField(customField.getCustomFieldPK().getFieldKey(), customField.getCustomFieldPK().getEntity(), selectedCaso);
//                    ccfs.add(newCasoCustomField);
//                }
//                selectedCaso.setCasoCustomFieldList(ccfs);
//            }
//        }

        if (selectedCaso.getCasoCustomFieldList() == null) {
            selectedCaso.setCasoCustomFieldList(new ArrayList<CasoCustomField>());
        }

        try {
            if (selectedCaso.getTipoCaso() != null && selectedCaso.getTipoCaso().getCustomFieldList() != null) {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "ManagerCasos.prepareCustomFields:{0}", selectedCaso.getTipoCaso().getCustomFieldList());
                for (CustomField customField : selectedCaso.getTipoCaso().getCustomFieldList()) {
                    CasoCustomField casoCustomField = new CasoCustomField(customField.getCustomFieldPK().getFieldKey(), customField.getCustomFieldPK().getEntity(), selectedCaso);
                    if (!selectedCaso.getCasoCustomFieldList().contains(casoCustomField)) {
                        casoCustomField.setCustomField(customField);
                        selectedCaso.getCasoCustomFieldList().add(casoCustomField);
                        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "add:{0}", casoCustomField);
//                        merge = true;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(CasoController.class.getName()).log(Level.SEVERE, "error at getItemsSubEstadoCasoAvailableSelectOneCasoAbierto", ex);
        }

    }

    private boolean crearNotaDesdeEmail(Caso caso, EmailMessage item) throws Exception {
        boolean retorno = false;
        if (caso != null) {
            caso.setRevisarActualizacion(true);

            Nota nota = new Nota();
            nota.setVisible(true);//cliente, nota publica
            nota.setEnviadoPor(item.getFromEmail().toLowerCase().trim());
            nota.setIdCanal(EnumCanal.CORREO.getCanal());
            String textoBody = item.getText();
//            textoBody = parseHtmlToText(textoBody);
            if (textoBody != null) {
                nota.setTexto(HtmlUtils.stripInvalidMarkup(textoBody));
            }
            nota.setIdCaso(caso);
            nota.setCreadaPor(EnumUsuariosBase.SISTEMA.getUsuario());
            nota.setFechaCreacion(Calendar.getInstance().getTime());
            if (item.getSubject().startsWith("Undeliverable:")) {
                nota.setTipoNota(EnumTipoNota.RESPUESTA_SERVIDOR.getTipoNota());
                nota.setTexto("El servidor de correos comuníca que su respuesta "
                        + "anterior no pudo ser entregada al destinatario");
            } else {
                nota.setTipoNota(EnumTipoNota.RESPUESTA_DE_CLIENTE.getTipoNota());

                StringBuilder attachmentsNames = new StringBuilder();
                for (EmailAttachment attachment : item.getAttachments()) {
                    while (!agregarAdjunto(attachment, caso)) {
                        //System.out.print(".");
                    }
                    if (attachment.getContentId() == null) {
                        attachmentsNames.append(attachment.getName()).append("<br/>");
                    }
                }
                if (!attachmentsNames.toString().isEmpty()) {
                    StringBuilder textoNota = new StringBuilder(nota.getTexto());
                    textoNota.append("<br/><div>Adjuntos incorporados:<br/>");
                    textoNota.append(attachmentsNames);
                    textoNota.append("</div>");
                    nota.setTexto(textoNota.toString());
                }
            }

            getJpaController().persist(nota);
            caso.getNotaList().add(nota);
            getJpaController().persist(createLogReg(caso, "Respuesta cliente", "nota id:" + nota.getIdNota(), ""));
            mergeCaso(caso, createLogReg(caso, "Notas", "Se agrega nota respuesta de cliente nota id:" + nota.getIdNota(), ""));
            retorno = true;
//            getJpaController().commitTransaction();
        }
        return retorno;
    }

//    private void enviarRespuestaAutomatica(Caso caso, final String emailCliente) {
//        try {
//            emailClient = MailClientFactory.getInstance(caso.getIdArea().getIdArea());
//            String mensaje_ = "";
//            String subject_ = "";
//            if (caso.getIdArea() != null) {
//                if (caso.getIdArea().getTextoRespAutomatica() != null) {
//                    mensaje_ = (ClippingsPlaceHolders.buildFinalText(caso.getIdArea().getTextoRespAutomatica(), caso));
//                }
//
//                if (caso.getIdArea().getSubjectRespAutomatica() != null) {
//                    subject_ = (ClippingsPlaceHolders.buildFinalText(caso.getIdArea().getSubjectRespAutomatica(), caso));
//                }
//            }
//
//            final String subject = subject_;
//            final String mensaje = mensaje_;
//
//            HelpDeskScheluder.scheduleTask("enviarRespuestaAutomatica" + System.currentTimeMillis(), HelpDeskScheluder.GRUPO_CORREO, new UnimailTask() {
//                public void execute() {
//                    try {
//                        if (ApplicationConfig.isAppDebugEnabled()) {
//                            Log.createLogger(this.getClass().getName()).logInfo("scheduling enviarRespuestaAutomatica, mailClient: " + emailClient);
//                        }
//                        emailClient.sendHTML(emailCliente, subject, mensaje, null);
//                    } catch (Exception ex) {
//                        Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }, Calendar.getInstance().getTime());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
//        }
//    }
    private boolean agregarAdjunto(EmailAttachment attachment, Caso caso) {
        try {
            String nombre = attachment.getName();
            nombre = nombre.substring(nombre.lastIndexOf(File.separator) + 1);
            nombre = nombre.substring(nombre.lastIndexOf('\\') + 1);
            crearAdjunto(attachment.getData(), attachment.getContentId(), caso, nombre, attachment.getMimeType(), attachment.getSize());
            return true;
        } catch (Exception e) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, null, e);
        }
        return false;
    }

    /**
     * @deprecated please add code into persist, merge, etc. after any db change
     * log it in JPAServiceFacade methods!!!!!!!!!!!!!!!! Crear un registro en
     * la tabla de log
     *
     * @param valorCampo Nombre del campo
     * @param newValue Nuevo valor
     * @param oldValue Valor antiguo
     */
    public AuditLog createLogReg(Caso caso, String valorCampo, String newValue, String oldValue) {
        if ((!valorCampo.toLowerCase().equalsIgnoreCase("fechaModif"))) {

            try {
                AuditLog audit = new AuditLog();
                UserSessionBean userSessionBean = (UserSessionBean) JsfUtil.getManagedBean("UserSessionBean");
//                Usuario idUser = EnumUsuariosBase.SISTEMA.getUsuario();
                String idUser = EnumUsuariosBase.SISTEMA.getUsuario().getIdUsuario();
                if (userSessionBean != null) {
                    Usuario idUserAux = userSessionBean.getCurrent();
                    if (idUserAux != null) {
                        idUser = idUserAux.getIdUsuario();
                    } else if (userSessionBean.getEmailCliente() != null) {
                        idUser = userSessionBean.getEmailCliente().getEmailCliente();
                    }
                }
                audit.setIdUser(idUser);
                audit.setFecha(Calendar.getInstance().getTime());
                audit.setTabla(Caso.class.getSimpleName());
                audit.setCampo(valorCampo);
                audit.setNewValue(newValue);
                audit.setOldValue(oldValue);
                audit.setIdCaso(caso.getIdCaso());
                if (caso.getOwner() == null) {
                    audit.setOwner(UsuarioController.SIN_PROPIETARIO);
                } else {
                    audit.setOwner(caso.getOwner().getIdUsuario());
                }
                return audit;
            } catch (Exception e) {
                Log.createLogger(CasoController.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        return null;
    }

    public AuditLog createLogComment(Caso caso, String comment) {
        return createLogReg(caso, "", comment, "");
    }

    public synchronized List<AuditLog> verificaCambios(Caso caso) {
        List<AuditLog> changeList = new LinkedList<AuditLog>();
        Caso casoOld = getJpaController().find(Caso.class, caso.getIdCaso());

        Method[] metodos = caso.getClass().getMethods();
        for (Method method : metodos) {
            if (method.getName().startsWith("get")) {
                try {
                    Object oldValue = method.invoke(casoOld);
                    Object newValue = method.invoke(caso);
                    String campo = method.getName().substring(3);
                    if ((oldValue != null) || (newValue != null)) {
                        AuditLog audit = null;
                        if (((oldValue == null) && (newValue != null))
                                || ((oldValue != null) && (newValue == null))) {
                            if (method.getName().contains("List")) {
                                audit = createLogReg(caso, campo, (newValue == null) ? ""
                                        : ((Collection) newValue).size() + " elementos",
                                        (oldValue == null) ? "" : ((Collection) oldValue).size() + " elementos");
                            } else {
                                audit = createLogReg(caso, campo,
                                        (newValue == null) ? "" : newValue.toString(),
                                        (oldValue == null) ? "" : oldValue.toString());
                            }
                        } else if (!oldValue.equals(newValue)) {
                            if (method.getName().contains("List")) {
                                audit = createLogReg(caso, campo,
                                        ((Collection) newValue).size() + " elemento(s)",
                                        ((Collection) oldValue).size() + " elemento(s)");
                            } else {
                                audit = createLogReg(caso, campo, newValue.toString(), oldValue.toString());
                            }
                        }
                        if (audit != null) {
                            changeList.add(audit);
                        }
                    }
                } catch (IllegalAccessException ex) {
                    Log.createLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Log.createLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Log.createLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return changeList;
    }

    public void crearAdjunto(byte[] bytearray, String contentId, Caso caso, String nombre, String mimeType, Long size) throws Exception {

        String fileName = nombre.trim().replace(" ", "_");
        Archivo archivo = new Archivo();
        archivo.setArchivo(bytearray);
        archivo.setContentType(mimeType);
        archivo.setFileName(fileName);
        try {
            archivo.setFormat(fileName.substring(fileName.lastIndexOf(".") + 1));
        } catch (Exception e) {
        }

        List<Attachment> col = caso.getAttachmentList();
        if (col == null) {
            col = new LinkedList<Attachment>();
        }

        Attachment attach = new Attachment();
        attach.setIdCaso(caso);
        attach.setNombreArchivo(fileName);
        attach.setFileSize(size);
        attach.setMimeType(mimeType);
        attach.setContentId(contentId);

        getJpaController().persist(attach);
        col.add(attach);
        caso.setAttachmentList(col);
        archivo.setIdAttachment(attach.getIdAttachment());
        getJpaController().persist(archivo);
//        getJpaController().persist(createLogReg(caso, "Archivo attachado", fileName, ""));
    }

//    private String parseHtmlToText(String textoTxt) {
//        textoTxt = HtmlUtils.stripInvalidMarkup(textoTxt);
//        return textoTxt;
//    }
    private void handleEmailAttachments(EmailMessage item, Caso caso) throws Exception {
        StringBuilder attachmentsNames = new StringBuilder();
//        System.out.println("El correo viene con " + item.getAttachments().size() + " Archivos adjuntos");
        for (EmailAttachment attachment : item.getAttachments()) {
            while (!agregarAdjunto(attachment, caso)) {
                //intentionally. bug fix for exchange api.
            }
            if (attachment.getContentId() == null) {
                attachmentsNames.append(attachment.getName()).append("<br/>");
            }
        }

        if (!attachmentsNames.toString().isEmpty()) {
            StringBuilder textoNota = new StringBuilder((caso.getDescripcion() == null) ? "" : caso.getDescripcion());
            textoNota.append("<br/><div>Adjuntos incorporados:<br/>");
            textoNota.append(attachmentsNames);
            textoNota.append("</div>");
            caso.setDescripcion(textoNota.toString());
            caso.setDescripcionTxt(HtmlUtils.stripInvalidMarkup(textoNota.toString()));
            mergeCaso(caso, createLogReg(caso, "Adjunto", "se agregan adjuntos al caso", ""));
        }
    }
}
