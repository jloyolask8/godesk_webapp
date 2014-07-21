/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.util;

import com.itcs.helpdesk.jsfcontrollers.util.ApplicationBean;
import com.itcs.helpdesk.jsfcontrollers.util.UserSessionBean;
import com.itcs.helpdesk.persistence.entities.Accion;
import com.itcs.helpdesk.persistence.entities.Area;
import com.itcs.helpdesk.persistence.entities.AuditLog;
import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.Categoria;
import com.itcs.helpdesk.persistence.entities.Condicion;
import com.itcs.helpdesk.persistence.entities.FieldType;
import com.itcs.helpdesk.persistence.entities.Grupo;
import com.itcs.helpdesk.persistence.entities.Prioridad;
import com.itcs.helpdesk.persistence.entities.ReglaTrigger;
import com.itcs.helpdesk.persistence.entities.TipoComparacion;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.persistence.entityenums.EnumFieldType;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAccion;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoComparacion;
import com.itcs.helpdesk.persistence.jpa.custom.CasoJPACustomController;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import com.itcs.helpdesk.persistence.utils.CasoChangeListener;
import com.itcs.helpdesk.persistence.utils.ComparableField;
import com.itcs.helpdesk.rules.Action;
import com.itcs.helpdesk.rules.actionsimpl.ScheduleJobAction;
import com.thoughtworks.xstream.XStream;
import java.beans.Expression;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.bean.ManagedProperty;
import javax.resource.NotSupportedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.mail.EmailException;

/**
 *
 * @author jorge
 * @author jonathan
 */
public class RulesEngine implements CasoChangeListener {

    @ManagedProperty(value = "#{UserSessionBean}")
    private UserSessionBean userSessionBean;

    private final JPAServiceFacade jpaController;
//    private final ManagerCasos managerCasos;
//    private EntityManagerFactory emf = null;

    private ApplicationBean applicationBean;

    public RulesEngine(JPAServiceFacade jpaController) {
        this.jpaController = jpaController;
//        this.managerCasos = managerCasos;
//        this.emf = emf;
    }

    private JPAServiceFacade getJpaController() {
        return jpaController;
    }

    @Override
    public void notifyAllWatchersOnline(Caso caso, String whoMadeTheChange, String message) {

        String[] userToBeNotified = null;//no-one

        String idCaso = caso.getIdCaso().toString();
        String originalTema = caso.getTema();

        String agentUserId = null;
        String customerUserId = null;
        //next, implement a list of watchers of the case.        

        if (caso.getOwner() != null && caso.getOwner().getIdUsuario() != null) {
            agentUserId = caso.getOwner().getIdUsuario();
        }

        if (caso.getEmailCliente() != null && caso.getEmailCliente().getEmailCliente() != null) {
            customerUserId = caso.getEmailCliente().getEmailCliente();
        }

        if (whoMadeTheChange == null) {
            //notify all, customer and agent
            userToBeNotified = new String[]{agentUserId, customerUserId};
        } else {

            if (whoMadeTheChange.equalsIgnoreCase(agentUserId)) {
                //agent made the change
                userToBeNotified = new String[]{customerUserId};
            } else if (whoMadeTheChange.equalsIgnoreCase(customerUserId)) {
                //customer made the change
                userToBeNotified = new String[]{agentUserId};
            }
        }

        if (applicationBean != null) {
            if (userToBeNotified != null) {
                for (String user : userToBeNotified) {
                    if (!StringUtils.isEmpty(user)) {
                        applicationBean.sendFacesMessageNotification(user, message);
                    }
                }

            }
        }
    }

    @Override
    public void casoCreated(Caso caso) {

        //TODO Permitir que el caso no tenga area! una regla podria ser que si el el area es null asignar DEFAULT.
        List<ReglaTrigger> listaSup = Collections.EMPTY_LIST;
        //Area en la que viene el caso si es que viene.
        Area areaDelCaso = caso.getIdArea();
        // a = EnumAreas.DEFAULT_AREA.getArea();
        if (areaDelCaso != null) {
            try {
                listaSup = (List<ReglaTrigger>) getJpaController().findReglasToExecute(areaDelCaso.getIdArea(), "CREATE");
            } catch (Exception ex) {
                Logger.getLogger(RulesEngine.class.getName()).log(Level.SEVERE, "error on finding rules! by Vista", ex);
            }

        } else {
            //Si el caso no trae area aplicarle todas las reglas de todas las areas!
            listaSup = (List<ReglaTrigger>) getJpaController().findAll(ReglaTrigger.class);
        }

        List<ReglaTrigger> lista;

        Log.createLogger(this.getClass().getName()).logInfo("*** Reglas Found for new caso: " + caso + " -> " + listaSup);
        do {

            lista = new LinkedList<ReglaTrigger>(listaSup);
            for (ReglaTrigger reglaTrigger : lista) {
                if (reglaTrigger.getReglaActiva()) {
//                    Log.createLogger(this.getClass().getName()).logInfo("*** Verificando regla -> " + reglaTrigger);
                    boolean aplica = false;
                    for (Condicion condicion : reglaTrigger.getCondicionList()) {
                        try {
                            aplica = verificarCondicion(condicion, caso, new ArrayList<AuditLog>());//no changes =)
                            if (!aplica) {
                                break;
                            }
                        } catch (Exception e) {
                            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "error on casoCreated Listener", e);
                            break;
                        }
                    }
                    if (aplica) {
                        Log.createLogger(this.getClass().getName()).logInfo("regla " + reglaTrigger.getIdTrigger() + " APLICA_AL_CASO " + caso.toString());
                        listaSup.remove(reglaTrigger);
                        for (Accion accion : reglaTrigger.getAccionList()) {
                            ejecutarAccion(accion, caso);
                        }
                    }

                }
            }
        } while (lista.size() > listaSup.size());

        if (ApplicationConfig.getInstance(getJpaController().getSchema()).isRealTimeNotifToAgentsEnabled()) {
            caso = getJpaController().getReference(Caso.class, caso.getIdCaso());
            //Online notification 
            if (caso.getOwner() != null) {
                String user = caso.getOwner().getIdUsuario();
                notifyAllWatchersOnline(caso, user, "Un nuevo caso ha sido asignado a ud. Tipo:" + (caso.getTipoCaso() != null ? caso.getTipoCaso().getNombre() : "caso") + " #" + caso.getIdCaso() + ": " + caso.getTema());
            }
        }

        if (ApplicationConfig.getInstance(getJpaController().getSchema()).isSendGroupNotifOnNewCaseEnabled()) {
            //Notify all agents in the groups
            if (caso.getIdProducto() != null) {
                for (Grupo grupo : caso.getIdProducto().getGrupoList()) {
                    MailNotifier.notifyGroupCasoReceived(grupo, caso, getJpaController().getSchema());
                }
            }
        }//TODO config what to do when case has no product when created

    }

    @Override
    public void casoChanged(Caso caso, List<AuditLog> changeList) {

        //TODO Permitir que el caso no tenga area! una regla podria ser que si el el area es null asignar DEFAULT.
        List<ReglaTrigger> listaSup = Collections.EMPTY_LIST;
        //Area en la que viene el caso si es que viene.
        Area areaDelCaso = caso.getIdArea();
        // a = EnumAreas.DEFAULT_AREA.getArea();
        if (areaDelCaso != null) {
            try {
                listaSup = (List<ReglaTrigger>) getJpaController().findReglasToExecute(areaDelCaso.getIdArea(), "UPDATE");
            } catch (Exception ex) {
                Logger.getLogger(RulesEngine.class.getName()).log(Level.SEVERE, "error on finding rules! by Vista", ex);
            }

        } else {
            //Si el caso no trae area aplicarle todas las reglas de todas las areas!
            listaSup = (List<ReglaTrigger>) getJpaController().findAll(ReglaTrigger.class);
        }

        List<ReglaTrigger> lista;

        Log.createLogger(this.getClass().getName()).logInfo("*** Reglas Found for caso updated: " + caso + " -> " + listaSup);

        do {
            lista = new LinkedList<ReglaTrigger>(listaSup);
            for (ReglaTrigger reglaTrigger : lista) {
                if (reglaTrigger.getReglaActiva()) {
                    boolean aplica = true;
                    for (Condicion condicion : reglaTrigger.getCondicionList()) {
//                        Log.createLogger(this.getClass().getName()).logInfo("***********" + condicion.toString());
                        try {
                            aplica = verificarCondicion(condicion, caso, changeList);
                            if (!aplica) {
//                                Log.createLogger(this.getClass().getName()).logInfo("regla " + reglaTrigger.getIdTrigger() + " no aplica al caso " + caso.getIdCaso());
//                                Log.createLogger(this.getClass().getName()).logInfo("condicion fallida " + condicion.getIdCampo().getIdCampo() + " " + condicion.getIdComparador().getSimbolo() + " " + condicion.getValor());
                                break;
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(RulesEngine.class.getName()).log(Level.SEVERE, "error on casoChanged verificarCondicion", ex);
                            break;
                        }

                    }
                    if (aplica) {
                        Log.createLogger(this.getClass().getName()).logInfo("regla " + reglaTrigger.getIdTrigger() + " APLICA_AL_CASO " + caso.toString());
                        listaSup.remove(reglaTrigger);
                        for (Accion accion : reglaTrigger.getAccionList()) {
                            ejecutarAccion(accion, caso);
                        }
                    }
                }
            }
        } while (lista.size() > listaSup.size());

        if (ApplicationConfig.getInstance(getJpaController().getSchema()).isRealTimeNotifToAgentsEnabled() || ApplicationConfig.getInstance(getJpaController().getSchema()).isRealTimeNotifToCustomerEnabled()) {
            //Online notification 
            if (changeList != null && !changeList.isEmpty()) {
                String user = changeList.get(0).getIdUser();
                notifyAllWatchersOnline(caso, user, "Uno de sus casos ha sido modificado, " + (caso.getTipoCaso() != null ? caso.getTipoCaso().getNombre() : "caso") + " #[" + caso.getIdCaso() + "]: " + caso.getTema());
            }
        }
    }

    public void applyRuleOnThisCasos(ReglaTrigger reglaTrigger, List<Caso> selectedCasos) {

        for (Caso caso : selectedCasos) {
            if (reglaTrigger.getReglaActiva()) {
                boolean aplica = false;
                for (Condicion condicion : reglaTrigger.getCondicionList()) {
                    try {
                        aplica = verificarCondicion(condicion, caso, new ArrayList<AuditLog>());//no changes =)
                        if (!aplica) {
                            break;
                        }
                    } catch (Exception e) {
                        Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "error applyRuleOnThisCasos", e);
                        break;
                    }
                }
                if (aplica) {
                    Log.createLogger(this.getClass().getName()).logInfo("regla " + reglaTrigger.getIdTrigger() + " APLICA_AL_CASO " + caso.toString());
                    for (Accion accion : reglaTrigger.getAccionList()) {
                        ejecutarAccion(accion, caso);
                    }
                }
            }
        }
    }

    /**
     * TODO implement changeList
     *
     * @param filtro
     * @param caso
     * @param changeList
     * @return
     * @throws NotSupportedException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    private boolean verificarCondicion(Condicion filtro, Caso caso, List<AuditLog> changeList) throws NotSupportedException, ClassNotFoundException, Exception {

        TipoComparacion operador = filtro.getIdComparador();

        Map<String, ComparableField> annotatedFields = getJpaController().getAnnotatedComparableFieldsMap(Caso.class);

        ComparableField comparableField = annotatedFields.get(filtro.getIdCampo());
        FieldType fieldType = comparableField.getFieldTypeId();

        String valorAttributo = filtro.getValor();

        if (operador == null || comparableField == null || valorAttributo == null || fieldType == null) {
            throw new NotSupportedException("La condicion no cumple con los requisitos minimos!");
        }

        Expression expresion;
        String methodName = "get" + WordUtils.capitalize(comparableField.getIdCampo());
        expresion = new Expression(caso, methodName, new Object[0]);
        expresion.execute();
        final Object value = expresion.getValue();

        if (ApplicationConfig.getInstance(getJpaController().getSchema()).isAppDebugEnabled()) {
            System.out.println("caso." + methodName + " = " + value);
        }

        if (fieldType.equals(EnumFieldType.TEXT.getFieldType()) || fieldType.equals(EnumFieldType.TEXTAREA.getFieldType())) {
            //El valor es de tipo String, usarlo tal como esta
            if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {

                if (value != null) {
                    return valorAttributo.equals((String) value);
                }

            } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
                if (value != null) {
                    return !valorAttributo.equals((String) value);
                }
            } else if (operador.equals(EnumTipoComparacion.CO.getTipoComparacion())) {

                if (value != null) {
                    final String patternToSearch = "\\b" + valorAttributo + "\\b";
                    Pattern p = Pattern.compile(patternToSearch, Pattern.CASE_INSENSITIVE);
                    //Match the given string with the pattern
                    Matcher m = p.matcher((String) value);
                    if (ApplicationConfig.getInstance(getJpaController().getSchema()).isAppDebugEnabled()) {
                        System.out.println("patternToSearch:" + patternToSearch);
                    }
                    return m.find();
//                    return ((String) expresion.getValue()).toLowerCase().contains(valorAttributo.toLowerCase());//removes case sensitive issue
                }

            } else if (operador.equals(EnumTipoComparacion.CT.getTipoComparacion())) {//Changed TO =)

                if (value != null) {
                    for (AuditLog auditLog : changeList) {
                        if (comparableField.getIdCampo().equalsIgnoreCase(auditLog.getCampo())) {
                            return valorAttributo.equals((String) value);
                        }
                    }
                }

            } else {
                throw new NotSupportedException("Comparador " + operador.getIdComparador() + " is not supported!!");
            }

        } else if (fieldType.equals(EnumFieldType.CALENDAR.getFieldType())) {
            //El valor es de tipo Fecha, usar el String parseado a una fecha

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            try {
                Date fecha1 = sdf.parse(valorAttributo);
                Date beanDate = ((Date) value);

                if (fecha1 != null && beanDate != null) {
                    if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {

                        return (fecha1.compareTo(beanDate) == 0);
                    } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
                        return (fecha1.compareTo(beanDate) != 0);
                    } else if (operador.equals(EnumTipoComparacion.LE.getTipoComparacion())) {

                        return (beanDate.getTime() <= fecha1.getTime()); //lessThanOrEqualTo

                    } else if (operador.equals(EnumTipoComparacion.GE.getTipoComparacion())) {
                        return (beanDate.getTime() >= fecha1.getTime());
                    } else if (operador.equals(EnumTipoComparacion.LT.getTipoComparacion())) {
                        return (beanDate.getTime() < fecha1.getTime());
                    } else if (operador.equals(EnumTipoComparacion.GT.getTipoComparacion())) {
                        return (beanDate.getTime() > fecha1.getTime());
                    } else if (operador.equals(EnumTipoComparacion.BW.getTipoComparacion())) {
                        Date fecha2 = sdf.parse(filtro.getValor2());
                        return ((beanDate.getTime() >= fecha1.getTime()) && (beanDate.getTime() <= fecha2.getTime()));
                    } else {
                        throw new NotSupportedException("Comparador " + operador.getIdComparador() + " is not supported!!");
                    }
                }
            } catch (ParseException ex) {
                //ignore and do not add this filter to the query
                Logger.getLogger(CasoJPACustomController.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else if (fieldType.equals(EnumFieldType.SELECTONE_ENTITY.getFieldType())) {

//            EntityManager em = null;
            try {
//                em = emf.createEntityManager();
                //El valor es el id de un entity, que tipo de Entity?= comparableField.tipo

                Class class_ = comparableField.getTipo();//Class.forName(comparableField.getTipo());
                Object oneEntity = value;

                //One or more values??
                if (operador.equals(EnumTipoComparacion.SC.getTipoComparacion())) {
                    //One or more values, as list select many.
                    List<String> valores = filtro.getValoresList();

                    if (oneEntity != null) {

                        return valores.contains(getJpaController().getIdentifier(oneEntity).toString());

                    } else {
                        return false;
                    }

                } else {

                    if (CasoJPACustomController.PLACE_HOLDER_ANY.equalsIgnoreCase(valorAttributo)) {
                        if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {
                            return (oneEntity != null);
                        } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
                            return (oneEntity == null);
                        } else if (operador.equals(EnumTipoComparacion.CT.getTipoComparacion())) {//Changed TO =)

                            if (value != null) {
                                for (AuditLog auditLog : changeList) {
                                    if (comparableField.getIdCampo().equalsIgnoreCase(auditLog.getCampo())) {
                                        return (oneEntity != null);
                                    }
                                }
                            }

                        } else {
                            throw new NotSupportedException("Tipo comparacion " + operador.getIdComparador() + " is not supported here!!");
                        }
                    } else if (CasoJPACustomController.PLACE_HOLDER_NULL.equalsIgnoreCase(valorAttributo)) {
                        if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {
                            return (oneEntity == null);
                        } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
                            return (oneEntity != null);
                        } else if (operador.equals(EnumTipoComparacion.CT.getTipoComparacion())) {//Changed TO =)

                            if (value != null) {
                                for (AuditLog auditLog : changeList) {
                                    if (comparableField.getIdCampo().equalsIgnoreCase(auditLog.getCampo())) {
                                        return (oneEntity == null);
                                    }
                                }
                            }

                        } else {
                            throw new NotSupportedException("Tipo comparacion " + operador.getIdComparador() + " is not supported here!!");
                        }
                    } else if (CasoJPACustomController.PLACE_HOLDER_CURRENT_USER.equalsIgnoreCase(valorAttributo)) {

                        if (userSessionBean != null && userSessionBean.getCurrent() != null) {
                            if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {
//                                return (oneEntity == null);
                                return (oneEntity != null ? userSessionBean.getCurrent().equals(oneEntity) : false);
                            } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
//                            return (oneEntity != null);
                                return (oneEntity != null ? !userSessionBean.getCurrent().equals(oneEntity) : false);
                            } else if (operador.equals(EnumTipoComparacion.CT.getTipoComparacion())) {//Changed TO =)

                                if (value != null) {
                                    for (AuditLog auditLog : changeList) {
                                        if (comparableField.getIdCampo().equalsIgnoreCase(auditLog.getCampo())) {
                                            return (oneEntity != null ? userSessionBean.getCurrent().equals(oneEntity) : false);
                                        }
                                    }
                                }

                            } else {
                                throw new NotSupportedException("Tipo comparacion " + operador.getIdComparador() + " is not supported here!!");
                            }
                        } else {
                            return false;
                        }

                    } else {
                        if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {
                            try {
                                final Object find = getJpaController().find(class_, valorAttributo);
                                if (find != null) {
                                    return find.equals(oneEntity);
                                } else {
                                    return false;
                                }

                            } catch (java.lang.IllegalArgumentException e) {
                                return getJpaController().find(class_, Integer.valueOf(valorAttributo)).equals(oneEntity);
                            }
                        } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
                            try {
                                return !getJpaController().find(class_, valorAttributo).equals(oneEntity);
                            } catch (java.lang.IllegalArgumentException e) {
                                return !getJpaController().find(class_, Integer.valueOf(valorAttributo)).equals(oneEntity);
                            }

                        } else if (operador.equals(EnumTipoComparacion.CT.getTipoComparacion())) {//Changed TO =)

                            if (value != null) {
                                for (AuditLog auditLog : changeList) {
                                    if (comparableField.getIdCampo().equalsIgnoreCase(auditLog.getCampo())) {
                                        try {
                                            return getJpaController().find(class_, valorAttributo).equals(oneEntity);
                                        } catch (java.lang.IllegalArgumentException e) {
                                            return getJpaController().find(class_, Integer.valueOf(valorAttributo)).equals(oneEntity);
                                        }
                                    }
                                }
                            }

                        } else {
                            throw new NotSupportedException("Tipo comparacion " + operador.getIdComparador() + " is not supported here!!");
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (fieldType.equals(EnumFieldType.SELECTONE_PLACE_HOLDER.getFieldType())) {

            Object oneEntity = value;

            if (CasoJPACustomController.PLACE_HOLDER_ANY.equalsIgnoreCase(valorAttributo)) {
                if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {
                    return (oneEntity != null);
                } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
                    return (oneEntity == null);
                } else {
                    throw new NotSupportedException("Tipo comparacion " + operador.getIdComparador() + " is not supported here!!");
                }
            } else if (CasoJPACustomController.PLACE_HOLDER_NULL.equalsIgnoreCase(valorAttributo)) {
                if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {
                    return (oneEntity == null);
                } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
                    return (oneEntity != null);
                } else {
                    throw new NotSupportedException("Tipo comparacion " + operador.getIdComparador() + " is not supported here!!");
                }
            }

        } else if (fieldType.equals(EnumFieldType.CHECKBOX.getFieldType())) {
            //Boolean comparation
            //El valor es de tipo boolean, usar el String parseado a un boolean

            try {
                Boolean valueBoolean = (Boolean) value;
                if (valueBoolean == null) {
                    valueBoolean = false;
                }

                boolean boolValue = Boolean.valueOf(valorAttributo);

                if (operador.equals(EnumTipoComparacion.EQ.getTipoComparacion())) {
                    return (valueBoolean == boolValue);
                } else if (operador.equals(EnumTipoComparacion.NE.getTipoComparacion())) {
                    return (valueBoolean != boolValue);
                } else if (operador.equals(EnumTipoComparacion.CT.getTipoComparacion())) {//Changed TO =)

                    if (valueBoolean != null) {
                        for (AuditLog auditLog : changeList) {
                            if (comparableField.getIdCampo().equalsIgnoreCase(auditLog.getCampo())) {
                                return (valueBoolean == boolValue);
                            }
                        }
                    }

                } else {
                    throw new NotSupportedException("Comparador " + operador.getIdComparador() + " is not supported in RulesEngine!!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        } else {
            throw new NotSupportedException("fieldType " + fieldType.getFieldTypeId() + " is not supported yet!!");
        }

        return false;
    }

    /**
     * TODO redo all this actions architecture based in Action execute class!
     * @param accion
     * @param caso 
     */
    private void ejecutarAccion(Accion accion, Caso caso) {
        if (accion.getIdTipoAccion().equals(EnumTipoAccion.CAMBIO_CAT.getTipoAccion())) {
            cambiarCategoria(accion, caso);
        } else if (accion.getIdTipoAccion().equals(EnumTipoAccion.ASIGNAR_A_GRUPO.getTipoAccion())) {
            asignarCasoAGrupo(accion, caso);
        } else if (accion.getIdTipoAccion().equals(EnumTipoAccion.ASIGNAR_A_AREA.getTipoAccion())) {
            asignarCasoArea(accion, caso);
        } else if (accion.getIdTipoAccion().equals(EnumTipoAccion.CUSTOM.getTipoAccion())) {
            executeCustomAction(accion, caso);
        } else if (accion.getIdTipoAccion().equals(EnumTipoAccion.SCHEDULE_ACTION.getTipoAccion())) {
            executeScheduleAction(accion, caso);
        } else if (accion.getIdTipoAccion().equals(EnumTipoAccion.ASIGNAR_A_USUARIO.getTipoAccion())) {
            asignarCasoAUsuario(accion, caso);
        } else if (accion.getIdTipoAccion().equals(EnumTipoAccion.CAMBIAR_PRIORIDAD.getTipoAccion())) {
            cambiarPrioridad(accion, caso);
        } else if (accion.getIdTipoAccion().equals(EnumTipoAccion.RECALCULAR_SLA.getTipoAccion())) {
            recalcularSLA(caso);
        } else if (accion.getIdTipoAccion().equals(EnumTipoAccion.ENVIAR_EMAIL.getTipoAccion())) {
            enviarCorreo(accion, caso);
        }
    }

    private void enviarCorreo(Accion accion, Caso caso) {
        try {
            XStream xstream = new XStream();
            EmailStruct emailStruct = (EmailStruct) xstream.fromXML(accion.getParametros());
            MailClientFactory.getInstance(getJpaController().getSchema(), caso.getIdArea().getIdArea())
                    .sendHTML(emailStruct.getToAdress(), ManagerCasos.formatIdCaso(caso.getIdCaso()) + " " + emailStruct.getSubject(), emailStruct.getBody(), null);
        } catch (EmailException ex) {
            Logger.getLogger(RulesEngine.class.getName()).log(Level.SEVERE, "enviarCorreo", ex);
        }
    }

    private Integer extractId(String parametros) {
        try {
            int index = parametros.lastIndexOf(" ID[");//no tocar cuero pico de pulga
            if (index >= 0) {
                String sub = parametros.substring(index).split("\\[")[1];
                String id = sub.replaceAll("]", "");
                return Integer.parseInt(id);
            }
        } catch (Exception e) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "No se pudo extraer ID de categoria", e);
        }
        return null;
    }

    private void asignarCasoAUsuarioConMenosCasos(Grupo grupo, Caso caso) throws Exception {
        Usuario usuarioConMenosCasos = null;
        for (Usuario usuario : grupo.getUsuarioList()) {
            if (usuario.getActivo()) {
                if (usuarioConMenosCasos == null) {
                    usuarioConMenosCasos = usuario;
                } else {
                    if (usuarioConMenosCasos.getCasoList().size() > usuario.getCasoList().size()) {
                        usuarioConMenosCasos = usuario;
                    }
                }
            }
        }
        if (usuarioConMenosCasos != null) {
            caso.setOwner(usuarioConMenosCasos);
            getJpaController().mergeCasoWithoutNotify(caso);

            if (ApplicationConfig.getInstance(getJpaController().getSchema()).isSendNotificationOnTransfer()) {
                try {
                    MailNotifier.notifyCasoAssigned(caso, null, getJpaController().getSchema());
                } catch (Exception ex) {
                    Logger.getLogger(RulesEngine.class.getName()).log(Level.SEVERE, "No se puede enviar notificacion por correo al agente asignado, dado que el area es null.", ex);
                }
            }

        }
    }

    private void cambiarCategoria(Accion accion, Caso caso) {
        try {
            int idCat = extractId(accion.getParametros());
            Categoria cat = getJpaController().find(Categoria.class, idCat);
            caso.setIdCategoria(cat);
            getJpaController().mergeCasoWithoutNotify(caso);
        } catch (Exception ex) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "cambiarCategoria", ex);
        }
    }

    private void asignarCasoAGrupo(Accion accion, Caso caso) {
        try {
            Grupo grupo = getJpaController().find(Grupo.class, accion.getParametros());
            asignarCasoAUsuarioConMenosCasos(grupo, caso);
        } catch (Exception ex) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "asignarCasoAGrupo", ex);
        }
    }

    private void asignarCasoAUsuario(Accion accion, Caso caso) {
        try {
            if (accion.getParametros() != null) {
                Usuario usuario = getJpaController().find(Usuario.class, accion.getParametros());
                caso.setOwner(usuario);
                getJpaController().mergeCasoWithoutNotify(caso);

                if (ApplicationConfig.getInstance(getJpaController().getSchema()).isSendNotificationOnTransfer()) {
                    try {
                        MailNotifier.notifyCasoAssigned(caso, null, getJpaController().getSchema());
                    } catch (Exception ex) {
                        Logger.getLogger(RulesEngine.class.getName()).log(Level.SEVERE, "asignarCasoAUsuario: No se puede enviar notificacion por correo al agente asignado, dado que el area es null.");
                    }
                }
            } else {
                Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "IllegalArgumentException: An instance of a null Usuario PK has been incorrectly provided for this find operation.");
            }

        } catch (Exception ex) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void asignarCasoArea(Accion accion, Caso caso) {
        try {
            Area area = getJpaController().find(Area.class, accion.getParametros());
            caso.setIdArea(area);
            getJpaController().mergeCasoWithoutNotify(caso);
        } catch (Exception ex) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "asignarCasoArea", ex);
        }
    }

    private void executeCustomAction(Accion accion, Caso caso) {
        try {
            String clazzName = accion.getParametros();

//            Constructor actionConstructor = Class.forName(clazzName).getConstructor(JPAServiceFacade.class, ManagerCasos.class);
//            Action actionInstance = (Action) actionConstructor.newInstance(getJpaController(), managerCasos);
            Action actionInstance = (Action) Class.forName(clazzName).newInstance();
//            actionInstance.setJpaController(getJpaController());
//            actionInstance.setManagerCasos(managerCasos);
            actionInstance.execute(caso, getJpaController().getSchema());
        } catch (Exception ex) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "executeCustomAction", ex);
        }
    }

    private void executeScheduleAction(Accion accion, Caso caso) {
        try {
            Action actionInstance = new ScheduleJobAction();
            actionInstance.setConfig(accion.getParametros());
            actionInstance.execute(caso, getJpaController().getSchema());
        } catch (Exception ex) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "executeCustomAction", ex);
        }
    }

    private void cambiarPrioridad(Accion accion, Caso caso) {
        try {
            Prioridad prioridad = getJpaController().find(Prioridad.class, accion.getParametros());
            caso.setIdPrioridad(prioridad);
            getJpaController().mergeCasoWithoutNotify(caso);
        } catch (Exception ex) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "cambiarPrioridad", ex);
        }
    }

    private void recalcularSLA(Caso caso) {
        try {
            ManagerCasos.calcularSLA(caso);
            getJpaController().mergeCasoWithoutNotify(caso);
        } catch (Exception ex) {
            Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "recalcularSLA", ex);
        }
    }

    /**
     * @param userSessionBean the userSessionBean to set
     */
    public void setUserSessionBean(UserSessionBean userSessionBean) {
        this.userSessionBean = userSessionBean;
    }

    /**
     * @param applicationBean the applicationBean to set
     */
    public void setApplicationBean(ApplicationBean applicationBean) {
        this.applicationBean = applicationBean;
    }
}
