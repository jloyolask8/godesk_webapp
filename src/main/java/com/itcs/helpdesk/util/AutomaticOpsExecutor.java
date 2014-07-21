package com.itcs.helpdesk.util;

import com.itcs.helpdesk.quartz.HelpDeskScheluder;
import com.itcs.helpdesk.persistence.entities.AppSetting;
import com.itcs.helpdesk.persistence.entities.Area;
import com.itcs.helpdesk.persistence.entities.Canal;
import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.Categoria;
import com.itcs.helpdesk.persistence.entities.EstadoCaso;
import com.itcs.helpdesk.persistence.entities.FieldType;
import com.itcs.helpdesk.persistence.entities.Funcion;
import com.itcs.helpdesk.persistence.entities.Grupo;
import com.itcs.helpdesk.persistence.entities.Prioridad;
import com.itcs.helpdesk.persistence.entities.Rol;
import com.itcs.helpdesk.persistence.entities.SubEstadoCaso;
import com.itcs.helpdesk.persistence.entities.TipoAccion;
import com.itcs.helpdesk.persistence.entities.TipoAlerta;
import com.itcs.helpdesk.persistence.entities.TipoCaso;
import com.itcs.helpdesk.persistence.entities.TipoComparacion;
import com.itcs.helpdesk.persistence.entities.TipoNota;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.persistence.entityenums.EnumAreas;
import com.itcs.helpdesk.persistence.entityenums.EnumCanal;
import com.itcs.helpdesk.persistence.entityenums.EnumCategorias;
import com.itcs.helpdesk.persistence.entityenums.EnumEstadoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumFieldType;
import com.itcs.helpdesk.persistence.entityenums.EnumFunciones;
import com.itcs.helpdesk.persistence.entityenums.EnumGrupos;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAccion;
import com.itcs.helpdesk.persistence.entityenums.EnumPrioridad;
import com.itcs.helpdesk.persistence.entityenums.EnumRoles;
import com.itcs.helpdesk.persistence.entityenums.EnumSettingsBase;
import com.itcs.helpdesk.persistence.entityenums.EnumSubEstadoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAlerta;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoCaso;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoComparacion;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoNota;
import com.itcs.helpdesk.persistence.entityenums.EnumUsuariosBase;
import com.itcs.helpdesk.persistence.jpa.exceptions.PreexistingEntityException;
import com.itcs.helpdesk.persistence.jpa.exceptions.RollbackFailureException;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.NoResultException;
import org.quartz.SchedulerException;

/**
 *
 * @author jorge
 */
public class AutomaticOpsExecutor {

    private final JPAServiceFacade jpaController;
//   
//    private final UserTransaction utx;
//    private final EntityManagerFactory emf;
    private ManagerCasos managerCasos;
     private boolean alertasAgendadas;
//    private final String schema;

    public AutomaticOpsExecutor(JPAServiceFacade jpaController) {
//            this.utx = utx;
        this.jpaController = jpaController;
        alertasAgendadas = false;
    }

    protected ManagerCasos getManagerCasos() {
        if (null == managerCasos) {
            managerCasos = new ManagerCasos();
            managerCasos.setJpaController(this.getJpaController());
        }
        return managerCasos;
    }
    
    public void agendarAlertasForAllCasos(String schema) {
        //System.out.println("calcularAlertas");
        if (!alertasAgendadas) {

            List<Caso> casos_pendiente = getJpaController().getCasoFindByEstadoAndAlerta(EnumEstadoCaso.ABIERTO.getEstado(), EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta());
//            System.out.println("encontrados "+casos.size()+" casos "+EnumTipoAlerta.TIPO_ALERTA_PENDIENTE+" que se debe agendar cambio de alerta");

            for (Caso caso : casos_pendiente) {
                try {
                    if ((caso.getEstadoAlerta().getIdalerta().equals(EnumTipoAlerta.TIPO_ALERTA_PENDIENTE.getTipoAlerta().getIdalerta()))
                            && (caso.getNextResponseDue().after(Calendar.getInstance().getTime()))) {
                        HelpDeskScheluder.scheduleAlertaPorVencer(schema, caso.getIdCaso(), getManagerCasos().calculaCuandoPasaAPorVencer(caso));
                    }

                    //Siempre se debe agendar el cambio a caso vencido para cuando se acabe el plazo para responder el caso
//                    HelpDeskScheluder.scheduleAlertaVencido(schema, caso.getIdCaso(), caso.getNextResponseDue());
//                    agendarCambioEstadoAlerta(schema, caso);
                } catch (SchedulerException ex) {
                    Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "agendarAlertasForAllCasos -> pendientes", ex);
                }
            }

            List<Caso> casos_por_vencer = getJpaController().getCasoFindByEstadoAndAlerta(EnumEstadoCaso.ABIERTO.getEstado(),
                    EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta());
//            System.out.println("encontrados "+casos.size()+" casos "+EnumTipoAlerta.TIPO_ALERTA_POR_VENCER+" que se debe agendar cambio de alerta");
            for (Caso caso : casos_por_vencer) {
                try {
                    HelpDeskScheluder.scheduleAlertaVencido(schema, caso.getIdCaso(), caso.getNextResponseDue());
//                    agendarCambioEstadoAlerta(schema, caso);
                } catch (SchedulerException ex) {
                    Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "agendarAlertasForAllCasos -> por vencer", ex);
                }
            }
            
            alertasAgendadas = true;

        }
    }

//    public void agendarAgendarAlertas(String schema) throws SchedulerException {
//        Calendar calendario = Calendar.getInstance();
//        calendario.add(Calendar.SECOND, 10);
//        JobDetail job = JobBuilder.newJob(AgendarAlertasCasosJob.class).withIdentity(AgendarAlertasCasosJob.JOB_ID_PREFFIX, HelpDeskScheluder.GRUPO_ALERTAS).build();
//        job.getJobDataMap().put(GoDeskDatabaseJob.ID_TENANT, schema);
//        HelpDeskScheluder.schedule(job, HelpDeskScheluder.GRUPO_ALERTAS, calendario.getTime());
//    }

    public void verificaDatosBase() {
        System.out.println("verificaDatosBase()...");
        verificarAreas(this.getJpaController());
        verificarGrupos(this.getJpaController());
        verificarUsuarios(this.getJpaController());
        verificarCategorias(this.getJpaController());
        verificarTiposAlerta(this.getJpaController());
        verificarFunciones(this.getJpaController());
        verificarRoles(this.getJpaController());
        verificarEstadosCaso(this.getJpaController());
        verificarSubEstadosCaso(this.getJpaController());
        verificarTipoCaso(this.getJpaController());
        verificarCanales(this.getJpaController());
        verificarPrioridades(this.getJpaController());
        verificarTiposNota(this.getJpaController());
        verificarFieldTypes(this.getJpaController());
        verificarTipoComparacion(this.getJpaController());
        verificarTipoAcciones(this.getJpaController());
        verificarSettingsBase(this.getJpaController());

    }

//    private void printOutContraintViolation(ConstraintViolationException ex, String classname) {
//        Set<ConstraintViolation<?>> set = (ex).getConstraintViolations();
//        for (ConstraintViolation<?> constraintViolation : set) {
//            Log.createLogger(classname).logInfo("leafBean class: " + constraintViolation.getLeafBean().getClass());
//            Log.createLogger(classname).logInfo("anotacion: " + constraintViolation.getConstraintDescriptor().getAnnotation().toString() + " value:" + constraintViolation.getInvalidValue());
//        }
//    }
//
//    public void exceptionThreatment(Exception ex, String classname) {
//        if (ex instanceof ConstraintViolationException) {
//            printOutContraintViolation((ConstraintViolationException) ex, classname);
//        }
//        if (ex.getCause() instanceof ConstraintViolationException) {
//            printOutContraintViolation((ConstraintViolationException) (ex.getCause()), classname);
//        }
//        Log.createLogger(classname).log(Level.SEVERE, "exceptionThreatment", ex);
//    }
    private void verificarSettingsBase(JPAServiceFacade jpaController) {
        for (EnumSettingsBase enumSettingsBase : EnumSettingsBase.values()) {
            try {
                if (null == jpaController.find(AppSetting.class, enumSettingsBase.getAppSetting().getSettingKey())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existen settings " + enumSettingsBase.getAppSetting().getSettingKey() + ", se creara ahora");
                try {
                    jpaController.persist(enumSettingsBase.getAppSetting());
                } catch (PreexistingEntityException pre) {
                    //ignore if already exists
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarUsuarios(JPAServiceFacade jpaController) {
        try {
            Usuario usuarioSistema = jpaController.find(Usuario.class, EnumUsuariosBase.SISTEMA.getUsuario().getIdUsuario());
            if (null == usuarioSistema) {
                throw new NoResultException("No existe usuario SISTEMA");
            }
        } catch (NoResultException ex) {
            try {
                Log.createLogger(this.getClass().getName()).logSevere("No existe usuario SISTEMA, se creara ahora");
                jpaController.persist(EnumUsuariosBase.SISTEMA.getUsuario());
            } catch (Exception e) {
                Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private void verificarTiposAlerta(JPAServiceFacade jpaController) {
        for (EnumTipoAlerta tipoAlerta : EnumTipoAlerta.values()) {
            try {
                if (null == jpaController.find(TipoAlerta.class, tipoAlerta.getTipoAlerta().getIdalerta())) {
                    throw new NoResultException();
                }

            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe tipo alerta " + tipoAlerta.getTipoAlerta().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(tipoAlerta.getTipoAlerta());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarFunciones(JPAServiceFacade jpaController) {
        for (EnumFunciones funcion : EnumFunciones.values()) {

            if (null == jpaController.find(Funcion.class, funcion.getFuncion().getIdFuncion())) {
                try {
                    Log.createLogger(this.getClass().getName()).logSevere("No existe funcion " + funcion.getFuncion().getNombre() + ", se creara ahora");
                    jpaController.persist(funcion.getFuncion());
                } catch (PreexistingEntityException ex) {
                    Logger.getLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (RollbackFailureException ex) {
                    Logger.getLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    private void verificarRoles(JPAServiceFacade jpaController) {
        for (EnumRoles enumRol : EnumRoles.values()) {
            try {
                if (null == jpaController.find(Rol.class, enumRol.getRol().getIdRol())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe rol " + enumRol.getRol().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumRol.getRol());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, "verificarRoles", e);
                }
            }
        }
    }

    private void verificarCategorias(JPAServiceFacade jpaController) {
        for (EnumCategorias enumCat : EnumCategorias.values()) {
            try {
                if (enumCat.isPersistente()) {
                    if (null == jpaController.find(Categoria.class, enumCat.getCategoria().getIdCategoria())) {
                        throw new NoResultException();
                    }
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe categoria " + enumCat.getCategoria().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumCat.getCategoria());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarAreas(JPAServiceFacade jpaController) {
        for (EnumAreas enumArea : EnumAreas.values()) {
            try {
                if (null == jpaController.find(Area.class, enumArea.getArea().getIdArea())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe area " + enumArea.getArea().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumArea.getArea());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarGrupos(JPAServiceFacade jpaController) {
        for (EnumGrupos enumGrupos : EnumGrupos.values()) {
            try {
                if (null == jpaController.find(Grupo.class, enumGrupos.getGrupo().getIdGrupo())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe grupo " + enumGrupos.getGrupo().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumGrupos.getGrupo());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarEstadosCaso(JPAServiceFacade jpaController) {
        for (EnumEstadoCaso enumEstado : EnumEstadoCaso.values()) {
            try {
                if (null == jpaController.find(EstadoCaso.class, enumEstado.getEstado().getIdEstado())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe estado " + enumEstado.getEstado().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumEstado.getEstado());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarSubEstadosCaso(JPAServiceFacade jpaController) {
        for (EnumSubEstadoCaso enumSubEstado : EnumSubEstadoCaso.values()) {
            try {
                if (null == jpaController.find(SubEstadoCaso.class, enumSubEstado.getSubEstado().getIdSubEstado())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe sub estado " + enumSubEstado.getSubEstado().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumSubEstado.getSubEstado());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarTipoCaso(JPAServiceFacade jpaController) {
        for (EnumTipoCaso enumTipoCaso : EnumTipoCaso.values()) {
            try {
                if (null == jpaController.find(TipoCaso.class, enumTipoCaso.getTipoCaso().getIdTipoCaso())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe el tipo caso " + enumTipoCaso.getTipoCaso().getIdTipoCaso() + ", se creara ahora");
                try {
                    jpaController.persist(enumTipoCaso.getTipoCaso());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarCanales(JPAServiceFacade jpaController) {
        for (EnumCanal enumCanal : EnumCanal.values()) {
            try {
                if (null == jpaController.find(Canal.class, enumCanal.getCanal().getIdCanal())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe canal " + enumCanal.getCanal().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumCanal.getCanal());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarPrioridades(JPAServiceFacade jpaController) {
        for (EnumPrioridad enumPrioridad : EnumPrioridad.values()) {
            try {
                Prioridad prioridad = jpaController.find(Prioridad.class, enumPrioridad.getPrioridad().getIdPrioridad());
                if (prioridad == null) {
                    Log.createLogger(this.getClass().getName()).logSevere("No existe prioridad " + enumPrioridad.getPrioridad().getNombre() + ", se creara ahora");
                    jpaController.persist(enumPrioridad.getPrioridad());
                } else {
                    jpaController.merge(enumPrioridad.getPrioridad());
                }
            } catch (Exception e) {
                Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private void verificarTiposNota(JPAServiceFacade jpaController) {
        for (EnumTipoNota enumTipoNota : EnumTipoNota.values()) {
            try {
                if (null == jpaController.find(TipoNota.class, enumTipoNota.getTipoNota().getIdTipoNota())) {
                    throw new NoResultException();
                } else {
                    jpaController.merge(enumTipoNota.getTipoNota());
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe tipo nota " + enumTipoNota.getTipoNota().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumTipoNota.getTipoNota());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            } catch (Exception ex) {
                Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void verificarTipoAcciones(JPAServiceFacade jpaController) {
        for (EnumTipoAccion enumTipoAccion : EnumTipoAccion.values()) {
            try {
                if (null == jpaController.find(TipoAccion.class, enumTipoAccion.getTipoAccion().getIdTipoAccion())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe nombre de accion " + enumTipoAccion.getTipoAccion().getNombre() + ", se creara ahora");
                try {
                    jpaController.persist(enumTipoAccion.getTipoAccion());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarFieldTypes(JPAServiceFacade jpaController) {
        for (EnumFieldType fieldType : EnumFieldType.values()) {
            try {
                if (null == jpaController.find(FieldType.class, fieldType.getFieldType().getFieldTypeId())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe nombre de fieldType " + fieldType.getFieldType().getFieldTypeId() + ", se creara ahora!!");
                try {
                    jpaController.persist(fieldType.getFieldType());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void verificarTipoComparacion(JPAServiceFacade jpaController) {
        for (EnumTipoComparacion enumTipoComparacion : EnumTipoComparacion.values()) {
            try {
                if (null == jpaController.find(TipoComparacion.class, enumTipoComparacion.getTipoComparacion().getIdComparador())) {
                    throw new NoResultException();
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).logSevere("No existe nombre de tipo comparacion " + enumTipoComparacion.getTipoComparacion().getIdComparador() + ", se creara ahora");
                try {
                    jpaController.persist(enumTipoComparacion.getTipoComparacion());
                } catch (Exception e) {
                    Log.createLogger(AutomaticOpsExecutor.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }
    }

    /**
     * @return the jpaController
     */
    public JPAServiceFacade getJpaController() {
        return jpaController;
    }

}
