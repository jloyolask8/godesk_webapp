package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.persistence.entities.Producto;
import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.jsfcontrollers.util.PaginationHelper;
import com.itcs.helpdesk.persistence.entities.Componente;
import com.itcs.helpdesk.persistence.entities.SubComponente;
import com.itcs.helpdesk.persistence.jpa.ProductoJpaController;
import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import com.itcs.helpdesk.util.ApplicationConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.persistence.NoResultException;
import jxl.CellReferenceHelper;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.SelectableDataModel;
import org.primefaces.model.UploadedFile;

@ManagedBean(name = "productoController")
@SessionScoped
public class ProductoController extends AbstractManagedBean<Producto> implements Serializable {

    @ManagedProperty(value = "#{componenteController}")
    private ComponenteController componenteController;
//    private Producto current;
    private Componente currentComponente = new Componente();
    private SubComponente currentSubComponente = new SubComponente();
//    private Producto[] selectedItems;
//    private transient DataModel items = null;
//    private transient PaginationHelper pagination;
    private int selectedItemIndex;
    //cell postion, for buld load.
    private String cellPositionProduct = "A";
    private String cellPositionComponent = "B";
    private String cellPositionSubComponentId = "C";
    private String cellPositionSubComponentName = "D";
    private String cellPositionSubComponentDesc = "E";
    private transient UploadedFile uploadFile;
    private List<Producto> bulkLoadedProductos;
    private List<Producto> bulkLoadedProductosWithErrors;
    private List<Componente> bulkLoadedComponentes;
    private List<Componente> bulkLoadedComponentesWithErrors;
    private List<SubComponente> bulkLoadedSubComponentes;
    private List<SubComponente> bulkLoadedSubComponenteWithErrors;
    private transient Map<String, Producto> persistentProductsMap = new HashMap<String, Producto>();
    private transient Map<String, Producto> bulkLoadedProductoMap = new HashMap<String, Producto>();
    private transient Map<String, Componente> bulkLoadedComponenteMap = new HashMap<String, Componente>();
    private transient Map<String, SubComponente> bulkLoadedSubComponenteMap = new HashMap<String, SubComponente>();

    private int tabActiveIndex;

    public ProductoController() {
        super(Producto.class);
    }

//    public Producto getSelected() {
//        if (current == null) {
//            current = new Producto();
//            selectedItemIndex = -1;
//        }
//        return current;
//    }
//
//    public void setSelected(Producto item) {
//        current = item;
//    }
    public String prepareCreateMasivo() {
        return "/script/producto/bulkLoad";
    }

    public void handleFileUpload() {
        handleFileUpload(null);
    }

    public void handleFileUpload(FileUploadEvent event) {

        persistentProductsMap = new HashMap<String, Producto>();
        int countSubComp = 0;
//        uploadFile = event.getFile();

        if (uploadFile != null) {

            FacesMessage msg = new FacesMessage("Archivo subido exitósamente", uploadFile.getFileName());
            FacesContext.getCurrentInstance().addMessage(null, msg);

            WorkbookSettings ws = new WorkbookSettings();
            ws.setEncoding("Cp1252");

            Workbook w;
            try {

                w = Workbook.getWorkbook(uploadFile.getInputstream(), ws);
                // Get the first sheet
                Sheet sheet = w.getSheet(0);
                // Loop over first 10 column and lines

                bulkLoadedProductoMap = new HashMap<String, Producto>();

                bulkLoadedComponenteMap = new HashMap<String, Componente>();

                bulkLoadedSubComponenteMap = new HashMap<String, SubComponente>();

                for (int rowIndex = 1; rowIndex < sheet.getRows(); rowIndex++) {

                    String nombreProducto = sheet.getCell(CellReferenceHelper.getColumn(cellPositionProduct), rowIndex).getContents();
                    String idComponente = sheet.getCell(CellReferenceHelper.getColumn(cellPositionComponent), rowIndex).getContents();
                    String subComponentId = sheet.getCell(CellReferenceHelper.getColumn(cellPositionSubComponentId), rowIndex).getContents();
                    String subComponentName = sheet.getCell(CellReferenceHelper.getColumn(cellPositionSubComponentName), rowIndex).getContents();
                    String subComponentDesc = sheet.getCell(CellReferenceHelper.getColumn(cellPositionSubComponentDesc), rowIndex).getContents();

                    Producto producto;
                    try {
                        producto = getJpaController().getProductoFindByNombre(nombreProducto);
                        if (producto != null) {
                            if (!persistentProductsMap.containsKey(producto.getIdProducto())) {
                                persistentProductsMap.put(producto.getIdProducto(), producto);
                            }
                        }
                    } catch (NoResultException nr) {
                        producto = new Producto(nombreProducto);
                        producto.setNombre(nombreProducto);
                    }

                    //Check whether is found
                    if (!bulkLoadedProductoMap.containsKey(producto.getIdProducto())) {
                        bulkLoadedProductoMap.put(producto.getIdProducto(), producto);
                    } else {
                        producto = bulkLoadedProductoMap.get(producto.getIdProducto());
                    }

                    Componente componente = new Componente(idComponente);
                    componente.setIdProducto(producto);
                    componente.setNombre(idComponente);

                    //Check whether is found
                    if (!bulkLoadedComponenteMap.containsKey(idComponente)) {
                        bulkLoadedComponenteMap.put(componente.getIdComponente(), componente);
                    } else {
                        componente = bulkLoadedComponenteMap.get(idComponente);
                    }

                    if (producto.getComponenteList() == null) {
                        producto.setComponenteList(new ArrayList<Componente>());
                    }
                    if (!producto.getComponenteList().contains(componente)) {
                        producto.getComponenteList().add(componente);
                    }

                    SubComponente subComponente = new SubComponente(subComponentId);
                    subComponente.setNombre(subComponentName);
                    subComponente.setDescripcion(subComponentDesc);
                    subComponente.setIdComponente(componente);

                    //Check whether is found
                    if (!bulkLoadedSubComponenteMap.containsKey(subComponentId)) {
                        bulkLoadedSubComponenteMap.put(subComponentId, subComponente);
                    } else {
                        subComponente = bulkLoadedSubComponenteMap.get(subComponentId);
                    }

                    if (componente.getSubComponenteList() == null) {
                        componente.setSubComponenteList(new ArrayList<SubComponente>());
                    }
                    if (!componente.getSubComponenteList().contains(subComponente)) {
                        componente.getSubComponenteList().add(subComponente);
                        countSubComp++;
                    }

                }

                bulkLoadedProductos = new ArrayList<Producto>(bulkLoadedProductoMap.values());

                bulkLoadedComponentes = new ArrayList<Componente>(bulkLoadedComponenteMap.values());

                bulkLoadedSubComponentes = new ArrayList<SubComponente>(bulkLoadedSubComponenteMap.values());

                System.out.println("countSubComp:" + countSubComp);

                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Extracción de datos del Archivo finalizada.", uploadFile.getFileName()));

            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Error al tratar de cargar la informacion desde el archivo, intente nuevamente."));
                e.printStackTrace();
            }

        } else {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Error al subir el archivo, intente nuevamente"));
        }

    }

    public boolean canEditProductId(Producto p) {
        if (persistentProductsMap.containsKey(p.getIdProducto())) {
            return false;
        } else {
            return true;
        }
    }

    public void handleChangeProductsName(Producto producto) {

        try {
            Producto persistent = getJpaController().getProductoFindByNombre(producto.getNombre());
            if (persistent != null) {//nombre exists
                if (!bulkLoadedProductoMap.containsKey(persistent.getIdProducto())) {//existent p is not in the list
                    //then modify the current product with persistent data.
                    producto.setIdProducto(persistent.getIdProducto());
                    producto.setNombre(persistent.getNombre());
                    producto.setDescripcion(persistent.getDescripcion());
                    producto.setComponenteList(persistent.getComponenteList());
                } else {
                    producto.setNombre("");
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("El nombre ingresado ya está siendo utilizado."));
                }
            }
        } catch (NoResultException nr) {//nombre do not exist
            //ignore
        }

    }

    public void handleChangeProductsId(Producto producto) {

        try {
            Producto persistent = getJpaController().find(Producto.class, producto.getIdProducto());
            if (persistent != null) {
                if (!bulkLoadedProductoMap.containsKey(persistent.getIdProducto())) {
                    producto.setIdProducto(persistent.getIdProducto());
                    producto.setNombre(persistent.getNombre());
                    producto.setComponenteList(persistent.getComponenteList());
                    producto.setDescripcion(persistent.getDescripcion());
                } else {
                    producto.setIdProducto("");
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("El Código ingresado ya está siendo utilizado."));
                }
            }
        } catch (NoResultException nr) {
            //ignore
        }
    }

    public void handleChangeProductsData() {
    }

    public String saveBulkImport() {
//        Map<String, Producto> bulkLoadedProductoErrorMap = new HashMap<String, Producto>();
//        Map<String, Componente> bulkLoadedComponenteErrorMap = new HashMap<String, Componente>();
//        Map<String, SubComponente> bulkLoadedSubComponenteErrorMap = new HashMap<String, SubComponente>();

        //--------
        bulkLoadedProductosWithErrors = new ArrayList<Producto>();

        int counterproducto = 0;

//        final ProductoJpaController pJpaController = getJpaController().getProductoJpaController();
        for (Producto producto : bulkLoadedProductos) {

            try {
                getJpaController().createOrMerge(producto);
                counterproducto++;
            } catch (Exception e) {
                bulkLoadedProductosWithErrors.add(producto);
                e.printStackTrace();
            }
        }

        addInfoMessage(counterproducto + " " + ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).getProductDescription() + "(s) fueron guardados de un total de " + bulkLoadedProductos.size());
        if (bulkLoadedProductosWithErrors.size() > 0) {
            addInfoMessage(bulkLoadedProductosWithErrors.size() + " " + ApplicationConfig.getInstance(getUserSessionBean().getTenantId()).getProductDescription() + "(s) tienen error" + bulkLoadedProductos.size());
        }

        //--------
        //        bulkLoadedComponentesWithErrors = new ArrayList<Componente>();
        //
        //        int counterComponente = 0;
        //
        //        final ComponenteJpaController cJpaController = getJpaController().getComponenteJpaController();
        //
        //        for (Componente componente : bulkLoadedComponentes) {
        //            try {
        //                cJpaController.create(componente);
        //                counterComponente++;
        //            } catch (Exception e) {
        //                bulkLoadedComponentesWithErrors.add(componente);
        //                e.printStackTrace();
        //            }
        //        }
        //
        //        JsfUtil.addSuccessMessage(counterComponente + " " + ApplicationConfig.getProductComponentDescription() + "(s) fueron guardados de un total de " + bulkLoadedComponentes.size());
        //--------
        //        bulkLoadedSubComponenteWithErrors = new ArrayList<SubComponente>();
        //
        //        int counterSubComponente = 0;
        //
        //        final SubComponenteJpaController scJpaController = getJpaController().getSubComponenteJpaController();
        //
        //        for (SubComponente subComponente : bulkLoadedSubComponentes) {
        //            try {
        //                scJpaController.create(subComponente);
        //                counterSubComponente++;
        //            } catch (Exception e) {
        //                bulkLoadedSubComponenteWithErrors.add(subComponente);
        //                e.printStackTrace();
        //            }
        //        }
        //
        //        JsfUtil.addSuccessMessage(counterComponente + " " + ApplicationConfig.getProductComponentDescription() + "(s) fueron guardados de un total de " + bulkLoadedComponentes.size());
        //--------
        try {
            prepareList();
            FacesContext.getCurrentInstance().getExternalContext().redirect("List.xhtml");
            //        return prepareList();
        } catch (IOException ex) {
            Logger.getLogger(ProductoController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    public int getTotalItemsCount() {
        return getJpaController().count(Producto.class).intValue();
    }

    public int getTotalComponentsItemsCount() {
        return getJpaController().count(Componente.class).intValue();
    }

    public int getTotalSubComponentsItemsCount() {
        return getJpaController().count(SubComponente.class).intValue();
    }

    public String prepareView() {
        if (current == null) {
            JsfUtil.addSuccessMessage("Es requerido que seleccione una fila para visualizar.");
            return "";
        }
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "/script/producto/View";
    }

    public String prepareCreate() {
        current = new Producto();
        selectedItemIndex = -1;
        return "/script/producto/Create";
    }

    public String create() {
        try {
            getJpaController().persist(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ProductoCreated"));
            return prepareEdit();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit(Producto item) {
        setSelected(item);
        return prepareEdit();
    }

    public String prepareCreateComponente() {
        currentComponente = new Componente();
        currentComponente.setIdProducto(current);
        return null;
    }

    public String prepareCreateSubComponente() {
        currentSubComponente = new SubComponente();
        currentSubComponente.setIdComponente(currentComponente);
        return null;
    }

    public void prepareEditComponente(Componente item) {
        currentComponente = item;
    }

    public void prepareEditSubComponente(SubComponente item) {
        currentSubComponente = item;
    }

    public void handleEditComponent() {
    }

    public void addComponentToProduct() {
        if (this.getSelected().getComponenteList() == null || this.getSelected().getComponenteList().isEmpty()) {
            this.getSelected().setComponenteList(new ArrayList<Componente>());
        }

        this.getSelected().getComponenteList().add(currentComponente);
        this.currentComponente = null;

    }

    public void addSubComponentToComponent() {
        if (this.getCurrentComponente().getSubComponenteList() == null || this.getCurrentComponente().getSubComponenteList().isEmpty()) {
            this.getCurrentComponente().setSubComponenteList(new ArrayList<SubComponente>());
        }

        this.getCurrentComponente().getSubComponenteList().add(currentSubComponente);
        this.currentSubComponente = null;

    }

    public String prepareList() {
        recreateModel();
        return "/script/producto/List";
    }

    public String prepareEdit() {
        if (current == null) {
            return prepareList();
        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "/script/producto/Edit";
    }

    public String update() {
        try {
            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ProductoUpdated"));
            return prepareList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        if (current == null) {
            return "";
        }
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return prepareList();
    }

    public String destroySelected() {

        if (current == null) {
            return "";
        }
        performDestroy();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        recreateModel();
        return "/script/producto/List";
    }

//    public String destroyAndView() {
//        performDestroy();
//        recreateModel();
//        updateCurrentItem();
//        if (selectedItemIndex >= 0) {
//            return "/script/producto/View";
//        } else {
//            // all items were removed - go back to list
//            recreateModel();
//            return "/script/producto/List";
//        }
//    }
    private void performDestroy() {
        try {
            getJpaController().remove(getEntityClass(), current.getIdProducto());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("ProductoDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

//    private void updateCurrentItem() {
//        int count = getJpaController().count(Producto.class).intValue();
//        if (selectedItemIndex >= count) {
//            // selected index cannot be bigger than number of items:
//            selectedItemIndex = count - 1;
//            // go to previous page if last page disappeared:
//            if (pagination.getPageFirstItem() >= count) {
//                pagination.previousPage();
//            }
//        }
//        if (selectedItemIndex >= 0) {
//            current = (Producto) getJpaController().queryByRange(Producto.class, 1, selectedItemIndex).get(0);
//        }
//    }
//
//    public DataModel getItems() {
//        if (items == null) {
//            items = getPagination().createPageDataModel();
//        }
//        return items;
//    }
    /**
     * @param componenteController the componenteController to set
     */
    public void setComponenteController(ComponenteController componenteController) {
        this.componenteController = componenteController;
    }

    /**
     * @return the currentComponente
     */
    public Componente getCurrentComponente() {
        return currentComponente;
    }

    /**
     * @param currentComponente the currentComponente to set
     */
    public void setCurrentComponente(Componente currentComponente) {
        this.currentComponente = currentComponente;
    }

    /**
     * @return the currentSubComponente
     */
    public SubComponente getCurrentSubComponente() {
        return currentSubComponente;
    }

    /**
     * @param currentSubComponente the currentSubComponente to set
     */
    public void setCurrentSubComponente(SubComponente currentSubComponente) {
        this.currentSubComponente = currentSubComponente;
    }

    /**
     * @return the cellPositionProduct
     */
    public String getCellPositionProduct() {
        return cellPositionProduct;
    }

    /**
     * @param cellPositionProduct the cellPositionProduct to set
     */
    public void setCellPositionProduct(String cellPositionProduct) {
        this.cellPositionProduct = cellPositionProduct;
    }

    /**
     * @return the cellPositionComponent
     */
    public String getCellPositionComponent() {
        return cellPositionComponent;
    }

    /**
     * @param cellPositionComponent the cellPositionComponent to set
     */
    public void setCellPositionComponent(String cellPositionComponent) {
        this.cellPositionComponent = cellPositionComponent;
    }

    /**
     * @return the cellPositionSubComponentId
     */
    public String getCellPositionSubComponentId() {
        return cellPositionSubComponentId;
    }

    /**
     * @param cellPositionSubComponentId the cellPositionSubComponentId to set
     */
    public void setCellPositionSubComponentId(String cellPositionSubComponentId) {
        this.cellPositionSubComponentId = cellPositionSubComponentId;
    }

    /**
     * @return the cellPositionSubComponentName
     */
    public String getCellPositionSubComponentName() {
        return cellPositionSubComponentName;
    }

    /**
     * @param cellPositionSubComponentName the cellPositionSubComponentName to
     * set
     */
    public void setCellPositionSubComponentName(String cellPositionSubComponentName) {
        this.cellPositionSubComponentName = cellPositionSubComponentName;
    }

    /**
     * @return the cellPositionSubComponentDesc
     */
    public String getCellPositionSubComponentDesc() {
        return cellPositionSubComponentDesc;
    }

    /**
     * @param cellPositionSubComponentDesc the cellPositionSubComponentDesc to
     * set
     */
    public void setCellPositionSubComponentDesc(String cellPositionSubComponentDesc) {
        this.cellPositionSubComponentDesc = cellPositionSubComponentDesc;
    }

    /**
     * @return the bulkLoadedProductos
     */
    public List<Producto> getBulkLoadedProductos() {
        return bulkLoadedProductos;
    }

    /**
     * @param bulkLoadedProductos the bulkLoadedProductos to set
     */
    public void setBulkLoadedProductos(List<Producto> bulkLoadedProductos) {
        this.bulkLoadedProductos = bulkLoadedProductos;
    }

    /**
     * @return the bulkLoadedProductosWithErrors
     */
    public List<Producto> getBulkLoadedProductosWithErrors() {
        return bulkLoadedProductosWithErrors;
    }

    /**
     * @param bulkLoadedProductosWithErrors the bulkLoadedProductosWithErrors to
     * set
     */
    public void setBulkLoadedProductosWithErrors(List<Producto> bulkLoadedProductosWithErrors) {
        this.bulkLoadedProductosWithErrors = bulkLoadedProductosWithErrors;
    }

    /**
     * @return the bulkLoadedComponentes
     */
    public List<Componente> getBulkLoadedComponentes() {
        return bulkLoadedComponentes;
    }

    /**
     * @param bulkLoadedComponentes the bulkLoadedComponentes to set
     */
    public void setBulkLoadedComponentes(List<Componente> bulkLoadedComponentes) {
        this.bulkLoadedComponentes = bulkLoadedComponentes;
    }

    /**
     * @return the bulkLoadedComponentesWithErrors
     */
    public List<Componente> getBulkLoadedComponentesWithErrors() {
        return bulkLoadedComponentesWithErrors;
    }

    /**
     * @param bulkLoadedComponentesWithErrors the
     * bulkLoadedComponentesWithErrors to set
     */
    public void setBulkLoadedComponentesWithErrors(List<Componente> bulkLoadedComponentesWithErrors) {
        this.bulkLoadedComponentesWithErrors = bulkLoadedComponentesWithErrors;
    }

    /**
     * @return the bulkLoadedSubComponentes
     */
    public List<SubComponente> getBulkLoadedSubComponentes() {
        return bulkLoadedSubComponentes;
    }

    /**
     * @param bulkLoadedSubComponentes the bulkLoadedSubComponentes to set
     */
    public void setBulkLoadedSubComponentes(List<SubComponente> bulkLoadedSubComponentes) {
        this.bulkLoadedSubComponentes = bulkLoadedSubComponentes;
    }

    /**
     * @return the bulkLoadedSubComponenteWithErrors
     */
    public List<SubComponente> getBulkLoadedSubComponenteWithErrors() {
        return bulkLoadedSubComponenteWithErrors;
    }

    /**
     * @param bulkLoadedSubComponenteWithErrors the
     * bulkLoadedSubComponenteWithErrors to set
     */
    public void setBulkLoadedSubComponenteWithErrors(List<SubComponente> bulkLoadedSubComponenteWithErrors) {
        this.bulkLoadedSubComponenteWithErrors = bulkLoadedSubComponenteWithErrors;
    }

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

    @Override
    public Class getDataModelImplementationClass() {
        return ProductoDataModel.class;
    }

    /**
     * @return the tabActiveIndex
     */
    public int getTabActiveIndex() {
        return tabActiveIndex;
    }

    /**
     * @param tabActiveIndex the tabActiveIndex to set
     */
    public void setTabActiveIndex(int tabActiveIndex) {
        this.tabActiveIndex = tabActiveIndex;
    }

    @FacesConverter(forClass = Producto.class)
    public static class ProductoControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ProductoController controller = (ProductoController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "productoController");
            return controller.getJpaController().find(Producto.class, getKey(value));
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
            if (object instanceof Producto) {
                Producto o = (Producto) object;
                return getStringKey(o.getIdProducto());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + Producto.class.getName());
            }
        }
    }
}

class ProductoDataModel extends ListDataModel<Producto> implements SelectableDataModel<Producto> {

    public ProductoDataModel() {
        //nothing
    }

    public ProductoDataModel(List<Producto> data) {
        super(data);
    }

    @Override
    public Producto getRowData(String rowKey) {
        List<Producto> listOfProducto = (List<Producto>) getWrappedData();

        for (Producto obj : listOfProducto) {
            if (obj.getIdProducto().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(Producto classname) {
        return classname.getIdProducto();
    }
}
