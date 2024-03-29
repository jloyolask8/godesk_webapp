/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.quartz;

import com.itcs.helpdesk.persistence.entityenums.EnumTipoAlerta;
import com.itcs.helpdesk.util.Log;
import java.util.Calendar;
import java.util.Date;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class HelpDeskScheluder {

    /**
     * Once you obtain a scheduler using
     * StdSchedulerFactory.getDefaultScheduler(), your application will not
     * terminate until you call scheduler.shutdown(), because there will be
     * active threads.
     *
     */
    public static final String GRUPO_CORREO = "correo";
    public static final String GRUPO_CASOS = "casos";
    public static final String GRUPO_ALERTAS = "alertas";
    public static final String TRIGGER_NAME = "trigger_";
//    private static int CORRELATIVO = 0;
    static private Scheduler scheduler = null;
    public static final int INTERVAL_10_MIN = 600;

    public static synchronized Scheduler getInstance() throws SchedulerException {
        if (scheduler == null) {
//            SchedulerFactory sf = new StdSchedulerFactory();
            scheduler = StdSchedulerFactory.getDefaultScheduler();// sf.getScheduler();
            scheduler.start();

        }
        return scheduler;
    }

    public static synchronized void stop() throws SchedulerException {
        if (scheduler != null) {
            scheduler.shutdown(true);
        }
    }

    public static synchronized boolean unschedule(final JobKey jobKey) throws SchedulerException {
//        try {
        boolean unscheduled = false;
        if (getInstance().checkExists(jobKey)) {
            unscheduled = getInstance().deleteJob(jobKey);
//            if (ApplicationConfig.isAppDebugEnabled()) {
            Log.createLogger(HelpDeskScheluder.class.getName()).logInfo("unschedule jobKey:" + jobKey.getName() + "/" + jobKey.getGroup() + (unscheduled ? " succeeded." : "failed."));
//            }
        }

        return unscheduled;
//        } catch (SchedulerException ex) {
//            Log.createLogger(HelpDeskScheluder.class.getName()).log(Level.SEVERE, null, ex);
//            return false;
//        }
    }

    public static synchronized void scheduleToRunNowWithInterval(JobDetail job, Integer intervalInSeconds) throws SchedulerException {
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(TRIGGER_NAME + job.getKey().getName(), job.getKey().getGroup())
                .startNow().withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalInSeconds)
                        .repeatForever()).build();
        getInstance().scheduleJob(job, trigger);
    }

    protected static synchronized void scheduleToRunNow(JobDetail job) throws SchedulerException {
//        JobDetail job = JobBuilder.newJob(UnimailJob.class).withIdentity(jobId, group).build();
//        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(TRIGGER_NAME + job.getKey().getName(), job.getKey().getGroup())
//                .startAt(startDate).withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalInSeconds)
//                        .repeatForever()).build();
//        job.getJobDataMap().put(TASK_NAME, task);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TRIGGER_NAME + job.getKey().getName(), job.getKey().getGroup())
                .startNow().build();

        getInstance().scheduleJob(job, trigger);
    }

    public static synchronized void schedule(JobDetail job, Date startDate) throws SchedulerException {
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TRIGGER_NAME + job.getKey().getName(), job.getKey().getGroup())
                .startAt(startDate).build();

        getInstance().scheduleJob(job, trigger);
    }

    public static void scheduleSendMailNow(final String schema, final String idArea, final String mensajeFinal,
            final String to, final String subject) throws SchedulerException {

        System.out.println("scheduling SendMail job");
        final String jobId = SendMailJob.formatJobId(schema, idArea, to);
        final JobKey jobKey = JobKey.jobKey(jobId, HelpDeskScheluder.GRUPO_CORREO);
        HelpDeskScheluder.unschedule(jobKey);

        JobDetail job = JobBuilder.newJob(SendMailJob.class).withIdentity(jobId, HelpDeskScheluder.GRUPO_CORREO).build();

        job.getJobDataMap().put(AbstractGoDeskJob.ID_AREA, idArea);
        job.getJobDataMap().put(AbstractGoDeskJob.ID_TENANT, schema);
        job.getJobDataMap().put(SendMailJob.EMAILS_TO, to);
        job.getJobDataMap().put(SendMailJob.EMAIL_SUBJECT, subject);
        job.getJobDataMap().put(SendMailJob.EMAIL_TEXT, mensajeFinal);

        HelpDeskScheluder.scheduleToRunNowWithInterval(job, INTERVAL_10_MIN);//10 minutes
    }
    
    public static String scheduleSendMail(final String schema, final String idArea, final String mensajeFinal,
            final String to, final String subject, final Date whenToRun) throws SchedulerException {

        System.out.println("scheduling SendMail job");
        final String jobId = SendMailJob.formatJobId(schema, idArea, to);
        final JobKey jobKey = JobKey.jobKey(jobId, HelpDeskScheluder.GRUPO_CORREO);
        HelpDeskScheluder.unschedule(jobKey);

        JobDetail job = JobBuilder.newJob(SendMailJob.class).withIdentity(jobId, HelpDeskScheluder.GRUPO_CORREO).build();

        job.getJobDataMap().put(AbstractGoDeskJob.ID_AREA, idArea);
        job.getJobDataMap().put(AbstractGoDeskJob.ID_TENANT, schema);
        job.getJobDataMap().put(SendMailJob.EMAILS_TO, to);
        job.getJobDataMap().put(SendMailJob.EMAIL_SUBJECT, subject);
        job.getJobDataMap().put(SendMailJob.EMAIL_TEXT, mensajeFinal);

        HelpDeskScheluder.schedule(job, whenToRun);
        return jobId;
    }
    
     public static String scheduleEventReminderJob(final String schema, final String idArea, 
            final String to, final String eventId, String eventReminderId, final Date whenToRun) throws SchedulerException {

        System.out.println("scheduling ScheduleEventReminderJob job");
        final String jobId = ScheduleEventReminderJob.formatJobId(schema, idArea, to, eventId, eventReminderId);
        final JobKey jobKey = JobKey.jobKey(jobId, HelpDeskScheluder.GRUPO_CORREO);
        HelpDeskScheluder.unschedule(jobKey);

        JobDetail job = JobBuilder.newJob(ScheduleEventReminderJob.class).withIdentity(jobId, HelpDeskScheluder.GRUPO_CORREO).build();

        job.getJobDataMap().put(AbstractGoDeskJob.ID_AREA, idArea);
        job.getJobDataMap().put(AbstractGoDeskJob.ID_TENANT, schema);
        job.getJobDataMap().put(ScheduleEventReminderJob.EMAILS_TO, to);
        job.getJobDataMap().put(ScheduleEventReminderJob.EVENT_ID, eventId);
        job.getJobDataMap().put(ScheduleEventReminderJob.REMINDER_ID, eventReminderId);

        HelpDeskScheluder.schedule(job, whenToRun);
        return jobId;
    }

    public static void scheduleRevisarCorreo(String schema, String idArea, Integer intervalInSeconds) throws SchedulerException {

        final String downloadEmailJobId = DownloadEmailJob.formatJobId(schema, idArea);
        final JobKey jobKey = JobKey.jobKey(downloadEmailJobId, HelpDeskScheluder.GRUPO_CORREO);
        HelpDeskScheluder.unschedule(jobKey);

        JobDetail job = JobBuilder.newJob(DownloadEmailJob.class).withIdentity(downloadEmailJobId, HelpDeskScheluder.GRUPO_CORREO).build();

        job.getJobDataMap().put(AbstractGoDeskJob.ID_TENANT, schema);
        job.getJobDataMap().put(AbstractGoDeskJob.ID_AREA, idArea);
        job.getJobDataMap().put(AbstractGoDeskJob.INTERVAL_SECONDS, intervalInSeconds.toString());

        Calendar calendario = Calendar.getInstance();
        calendario.add(Calendar.SECOND, intervalInSeconds);

//        HelpDeskScheluder.schedule(job, calendario.getTime(), intervalInSeconds);
        HelpDeskScheluder.schedule(job, calendario.getTime());
    }

    public static void unscheduleCambioAlertas(String schema, Long idCaso) throws SchedulerException {
        TicketAlertStateChangeJob.unschedule(TicketAlertStateChangeJob.formatJobId(schema, idCaso, EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta().getIdalerta()));
        TicketAlertStateChangeJob.unschedule(TicketAlertStateChangeJob.formatJobId(schema, idCaso, EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta().getIdalerta()));
    }

    public static String scheduleActionClassExecutorJob(final String schema, final Long idCaso, final String actionClassName, final String params, final Date whenToRun) throws SchedulerException {
        final String time = String.valueOf( whenToRun.getTime() );
        final String jobId = ActionClassExecutorJob.formatJobId(schema, idCaso.toString(), actionClassName, time);

        JobDetail job = JobBuilder.newJob(ActionClassExecutorJob.class)
                .withIdentity(jobId, HelpDeskScheluder.GRUPO_CASOS).build();
        job.getJobDataMap().put(ActionClassExecutorJob.ID_TENANT, schema);
        job.getJobDataMap().put(ActionClassExecutorJob.ID_CASO, idCaso.toString());
        job.getJobDataMap().put(ActionClassExecutorJob.ACTION_CLASSNAME, actionClassName);
        job.getJobDataMap().put(ActionClassExecutorJob.ACTION_PARAMS, params);
        job.getJobDataMap().put(ActionClassExecutorJob.ID_TIME, time);

        HelpDeskScheluder.schedule(job, whenToRun);
        return jobId;

    }

    public static void scheduleAlertaVencido(final String schema, final Long idCaso, final Date whenToRun) throws SchedulerException {
        final String jobId = TicketAlertStateChangeJob.formatJobId(schema, idCaso, EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta().getIdalerta());

        TicketAlertStateChangeJob.unschedule(jobId);

        JobDetail job = JobBuilder.newJob(TicketAlertStateChangeJob.class)
                .withIdentity(jobId, HelpDeskScheluder.GRUPO_CASOS).build();
        job.getJobDataMap().put(TicketAlertStateChangeJob.ID_TENANT, schema);
        job.getJobDataMap().put(TicketAlertStateChangeJob.ID_CASO, idCaso.toString());
        job.getJobDataMap().put(TicketAlertStateChangeJob.ID_ESTADO_ALERTA, EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta().getIdalerta().toString());

        HelpDeskScheluder.schedule(job, whenToRun/*casoFinal.getNextResponseDue()*/);

//        HelpDeskScheluder.scheduleTask(casoFinal.getIdCaso().toString() + "b", HelpDeskScheluder.GRUPO_CASOS, new QuartzTask() {
//            @Override
//            public void execute() {
//                Caso caso = getJpaController().find(Caso.class, idCaso);
//                caso.setEstadoAlerta(getJpaController().find(TipoAlerta.class, EnumTipoAlerta.TIPO_ALERTA_VENCIDO.getTipoAlerta().getIdalerta()));
//                try {
//                    getJpaController().mergeCaso(caso, verificaCambios(caso));
//
//                } catch (Exception ex) {
//                    Log.createLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                try {
//                    HelpDeskScheluder.unscheduleTask(caso.getIdCaso().toString() + "b", HelpDeskScheluder.GRUPO_CASOS);
//                } catch (SchedulerException ex) {
//                    Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "unscheduleTask b " + caso.getIdCaso().toString(), ex);
//                }
//            }
//        }, casoFinal.getNextResponseDue());
    }

    public static synchronized void scheduleAlertaPorVencer(final String schema, final Long idCaso, final Date whenToRun) throws SchedulerException {

        final String jobId = TicketAlertStateChangeJob.formatJobId(schema, idCaso, EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta().getIdalerta());
        TicketAlertStateChangeJob.unschedule(jobId);

        JobDetail job = JobBuilder.newJob(TicketAlertStateChangeJob.class)
                .withIdentity(jobId, HelpDeskScheluder.GRUPO_CASOS).build();
        job.getJobDataMap().put(TicketAlertStateChangeJob.ID_TENANT, schema);
        job.getJobDataMap().put(TicketAlertStateChangeJob.ID_CASO, idCaso.toString());
        job.getJobDataMap().put(TicketAlertStateChangeJob.ID_ESTADO_ALERTA, EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta().getIdalerta().toString());

        HelpDeskScheluder.schedule(job, whenToRun);

//            HelpDeskScheluder.scheduleTask(casoFinal.getIdCaso().toString() + "a", HelpDeskScheluder.GRUPO_CASOS, new QuartzTask() {
//                @Override
//                public void execute() {
//                    Caso caso = getJpaController().find(Caso.class, idCaso);
//                    caso.setEstadoAlerta(getJpaController().find(TipoAlerta.class, EnumTipoAlerta.TIPO_ALERTA_POR_VENCER.getTipoAlerta().getIdalerta()));
//                    try {
//                        getJpaController().mergeCaso(caso, verificaCambios(caso));
//
//                    } catch (Exception ex) {
//                        Log.createLogger(CasoController.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                    try {
//                        HelpDeskScheluder.unscheduleTask(caso.getIdCaso().toString() + "a", HelpDeskScheluder.GRUPO_CASOS);
//                    } catch (SchedulerException ex) {
//                        Logger.getLogger(ManagerCasos.class.getName()).log(Level.SEVERE, "unscheduleTask a " + caso.getIdCaso().toString(), ex);
//                    }
//                }
//            }, calendario.getTime());
    }

    private HelpDeskScheluder() throws Exception {
        //se crea el scheduler
        //getScheduler();
    }
}
