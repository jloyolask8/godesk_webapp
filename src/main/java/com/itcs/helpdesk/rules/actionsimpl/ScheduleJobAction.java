/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.rules.actionsimpl;

import com.itcs.helpdesk.persistence.entities.Caso;
import com.itcs.helpdesk.persistence.entities.CasoCustomField;
import com.itcs.helpdesk.rules.Action;
import com.itcs.helpdesk.rules.ActionExecutionException;
import com.itcs.helpdesk.util.DateUtils;
import com.itcs.helpdesk.quartz.HelpDeskScheluder;
import com.itcs.helpdesk.util.Log;
import java.beans.Expression;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.quartz.SchedulerException;

/**
 *
 * @author jonathan
 */
public class ScheduleJobAction extends Action {

    public static final String DATE_FIELD_KEY = "dateFieldKey";
    public static final String ACTION_CLASS = "actionclass";

//    public ScheduleJobAction(JPAServiceFacade jpaController, ManagerCasos managerCasos) {
//        super(jpaController, managerCasos);
//    }

    @Override
    public void execute(final Caso caso, final String schema) throws ActionExecutionException {

        Log.createLogger(ScheduleJobAction.class.getName()).logSevere("ScheduleJobAction.execute on " + caso);

        String propertiesData = getConfig();
        if (StringUtils.isEmpty(propertiesData)) {
            throw new ActionExecutionException("Esta accion necesita parametros para poder executarse.");
        }
        Properties props = new Properties();
        try {
            // load a properties file for reading  
            props.load(new StringReader(propertiesData));
        } catch (IOException ex) {
            throw new ActionExecutionException("Error processing properties: " + propertiesData, ex);
        }

        String dateFieldKey = props.getProperty(DATE_FIELD_KEY);
        final String actionClassName = props.getProperty(ACTION_CLASS);

        if (StringUtils.isEmpty(dateFieldKey) || StringUtils.isEmpty(actionClassName)) {
            throw new ActionExecutionException("Esta accion necesita una Fecha y una Custom Action (Class Name) para poder executarse. data received:" + propertiesData);
        }

        try {
            Date scheduleDate = Calendar.getInstance().getTime();
            //need to extract the date from caso based on dateFieldKey
            Expression expresion;
            String methodName = "get" + WordUtils.capitalize(dateFieldKey);
            expresion = new Expression(caso, methodName, new Object[0]);
            try {
                expresion.execute();
                final Object value = expresion.getValue();
                scheduleDate = (Date) value;
            } catch (Exception ex) {
                Log.createLogger(ScheduleJobAction.class.getName()).logWarning(dateFieldKey + " is not a date or ITS A CUSTOM FIELD");

                for (CasoCustomField casoCustomField : caso.getCasoCustomFieldList()) {
                    try {
                        Log.createLogger(ScheduleJobAction.class.getName()).logSevere(dateFieldKey + " == " + casoCustomField);
                        if (casoCustomField.getFieldKey().equalsIgnoreCase(dateFieldKey)) {
                            //This is the one!
                            final String dateFormat = DateUtils.determineDateFormat(casoCustomField.getValor());
                            Log.createLogger(ScheduleJobAction.class.getName()).logWarning("dateFormat:" + dateFormat + ", dateVal:" + casoCustomField.getValor());
                            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                            scheduleDate = sdf.parse(casoCustomField.getValor());
                            break;
                        }
                    } catch (Exception e) {
                        Logger.getLogger(ScheduleJobAction.class.getName()).log(Level.SEVERE, "error on parse custom field supossed date:" + casoCustomField.getValor(), e);
                    }
                }
            }
            
            HelpDeskScheluder.scheduleActionClassExecutorJob(schema, caso.getIdCaso(), actionClassName, "", scheduleDate);

        } catch (SchedulerException ex) {
            throw new ActionExecutionException("Error al tratar de agendar Action en Quartz:" + props, ex);
        }



    }
}
