/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.quartz;

import com.itcs.helpdesk.persistence.entities.AuditLog;
import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.TipoAlerta;
import com.itcs.helpdesk.persistence.entityenums.EnumTipoAlerta;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

/**
 * TODO register CasoChangeListener in order to run/execute rules on this alert changes!!
 * @author jonathan
 */
public class TicketAlertStateChangeJob extends AbstractGoDeskJob implements Job {

    public static final String ID_CASO = "idCaso";
    public static final String ID_ESTADO_ALERTA = "idalerta";
    /**
     * {0} = schema {1} = caso# {2} = id alerta
     */
    private static final String JOB_ID = "%s_TicketAlertStateChange_%s_%s";

    public static String formatJobId(String schema, Long idCaso, Integer idalerta) {
        return String.format(JOB_ID, new Object[]{schema, idCaso.toString(), idalerta.toString()});
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
//        throw new UnsupportedOperationException("Not supported yet."); 

        try {
            JobDataMap map = context.getMergedJobDataMap();//.getJobDetail().getJobDataMap();
            if (map != null) {
                String schema = (String) map.get(ID_TENANT);
                String idCaso = (String) map.get(ID_CASO);
                String idalerta = (String) map.get(ID_ESTADO_ALERTA);
                if (!StringUtils.isEmpty(schema) && !StringUtils.isEmpty(idCaso) && !StringUtils.isEmpty(idalerta)) {
                    EntityManager em = createEntityManager(schema);
                    try {
                        final Long valueOfIdCaso = Long.valueOf(idCaso);
                        final Integer valueOfIdAlerta = Integer.valueOf(idalerta);

                        Caso caso = em.getReference(Caso.class, valueOfIdCaso);
                        caso.setEstadoAlerta(em.getReference(TipoAlerta.class, valueOfIdAlerta));
                        caso.setFechaModif(new Date());

                        em.merge(caso);

                        String idUser = context.getScheduler().getSchedulerName();
                        AuditLog audit = new AuditLog();
                        audit.setIdUser(idUser);
                        audit.setFecha(Calendar.getInstance().getTime());
                        audit.setTabla(Caso.class.getSimpleName());
                        audit.setNewValue("Estado de alerta pasa a " + caso.getEstadoAlerta().getNombre());
                        audit.setIdCaso(caso.getIdCaso());
                        if (caso.getOwner() == null) {
                            audit.setOwner(null);
                        } else {
                            audit.setOwner(caso.getOwner().getIdUsuario());
                        }

                        em.persist(audit);

                        unschedule(formatJobId(schema, valueOfIdCaso, valueOfIdAlerta));
                        
                        if (valueOfIdAlerta.equals(EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta().getIdalerta())) {
                            HelpDeskScheluder.scheduleAlertaVencido(schema, valueOfIdCaso, caso.getNextResponseDue());
                        }

                    } finally {
                        em.close();
                    }

                } else {
                    throw new IllegalStateException("los parametros proporcionados al Job CambiarEstadoAlertaCasoJob(schema, idCaso, idalerta) son illegales!");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "TicketAlertStateChangeJob.execute", ex);
        }
    }

    public static void unschedule(String formatJobId) throws SchedulerException {
//        final String formatJobId = formatJobId(schema, idCaso, idalerta);
        final JobKey jobKey = JobKey.jobKey(formatJobId, HelpDeskScheluder.GRUPO_CASOS);
        HelpDeskScheluder.unschedule(jobKey);
    }

    @Override
    protected String getGrupo() {
        return HelpDeskScheluder.GRUPO_CASOS;
    }

    @Override
    protected String getJobId() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
