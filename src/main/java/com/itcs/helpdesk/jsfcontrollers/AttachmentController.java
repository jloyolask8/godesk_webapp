package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.Attachment;
import com.itcs.helpdesk.util.Log;
import common.Logger;
import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.ListDataModel;
import org.primefaces.model.SelectableDataModel;
import org.primefaces.model.UploadedFile;


@ManagedBean(name = "attachmentController")
@RequestScoped
public class AttachmentController extends AbstractManagedBean<Attachment> implements Serializable {

//    private Attachment current;
//    private Attachment[] selectedItems;   
//    private int selectedItemIndex;

    private transient UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void subir(){

    }

    public AttachmentController() {
        super(Attachment.class);
        Logger.getLogger(this.getClass()).info("AttachmentController created");
    }


//    public String prepareCreate() {
//        current = new Attachment();
//        return "Create";
//    }
//
//    public String create() {
//        try {
//            getJpaController().persistAttachment(current);
//            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("AttachmentCreated"));
//            return prepareCreate();
//        } catch (Exception e) {
//            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
//            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
//            return null;
//        }
//    }
//
//    public String update() {
//        try {
//            getJpaController().mergeAttachment(current);
//            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("AttachmentUpdated"));
//            return "View";
//        } catch (Exception e) {
//            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
//            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
//            return null;
//        }
//    }

//    public String destroy() {
//        if (current == null) {
//            return "";
//        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
//        performDestroy();
//        recreateModel();
//        return "List";
//    }

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
//        return "List";
//    }

   

    private void performDestroy() {
        try {
            getJpaController().remove(Attachment.class, current.getIdAttachment());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("AttachmentDeleted"));
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

    

 

    @Override
    public Class getDataModelImplementationClass() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @FacesConverter(forClass = Attachment.class)
    public static class AttachmentControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AttachmentController controller = (AttachmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "attachmentController");
            return controller.getJpaController().find(Attachment.class, getKey(value));
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
            if (object instanceof Attachment) {
                Attachment o = (Attachment) object;
                return getStringKey(o.getIdAttachment());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + AttachmentController.class.getName());
            }
        }
    }
}
class AttachmentDataModel extends ListDataModel<Attachment> implements SelectableDataModel<Attachment> {

    public AttachmentDataModel() {
        //nothing
    }

    public AttachmentDataModel(List<Attachment> data) {
        super(data);
    }

    @Override
    public Attachment getRowData(String rowKey) {
        List<Attachment> listOfAttachment = (List<Attachment>) getWrappedData();

        for (Attachment obj : listOfAttachment) {
            if (obj.getIdAttachment().toString().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(Attachment classname) {
        return classname.getIdAttachment();
    }
}