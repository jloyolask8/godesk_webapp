package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.Categoria;
import com.itcs.helpdesk.persistence.entities.Grupo;
import com.itcs.helpdesk.persistence.entities.Producto;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.util.Log;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.ListDataModel;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.DualListModel;
import org.primefaces.model.SelectableDataModel;
import org.primefaces.model.TreeNode;

@ManagedBean(name = "grupoController")
@SessionScoped
public class GrupoController extends AbstractManagedBean<Grupo> implements Serializable {

//    private Grupo current;
//    private transient DataModel items = null;
//    private transient PaginationHelper pagination;
    private int selectedItemIndex;
    //private DualListModel<Categoria> categoriasDualListModel;
    private transient TreeNode[] selectedCategoriasNodes;
    private transient TreeNode treeNodeCategorias;
    private int categoriaOrdenMayor;
    private DualListModel<Usuario> usuariosDualListModel = new DualListModel<Usuario>();
    private DualListModel<Producto> productoDualListModel = new DualListModel<Producto>();

    public GrupoController() {
        super(Grupo.class);
    }

//    public Grupo getSelected() {
//        if (current == null) {
//            current = new Grupo();
//            selectedItemIndex = -1;
//        }
//        //System.out.println("getSelected: "+current.getIdGrupo());
//        return current;
//    }
//    public void setSelected(Grupo selected) {
//        this.current = selected;
//        updateCurrent();
//    }
    public TreeNode[] getSelectedCategoriasNodes() {
        if (this.selectedCategoriasNodes == null) {
            return null;
        } else {
            return this.selectedCategoriasNodes.clone();
        }
    }

    public void setSelectedCategoriasNodes(TreeNode[] selectedNodes) {
        this.selectedCategoriasNodes = selectedNodes.clone();
    }

//    private void updateCurrent() {
//        if (current != null) {
//            current = getJpaController().getGrupoFindByIdGrupo(current.getIdGrupo());
//        }
//    }
    public String prepareList() {
        recreateModel();
        return "/script/grupo/List";
    }

    public String prepareView() {
        if (getSelected() == null) {
            JsfUtil.addSuccessMessage("Se requiere que seleccione una fila para visualizar.");
            return "";
        }
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "/script/grupo/View";
    }

    public String prepareCreate() {
        current = new Grupo();
        selectedItemIndex = -1;
        setUsuariosDualListModel(new DualListModel<Usuario>((List<Usuario>) getJpaController().findAll(Usuario.class), new ArrayList<Usuario>()));
        setProductoDualListModel(new DualListModel<Producto>((List<Producto>) getJpaController().findAll(Producto.class), new ArrayList<Producto>()));
        //setCategoriasDualListModel(new DualListModel<Categoria>(getJpaController().getCategoriaFindAll(), new ArrayList<Categoria>()));
        return "/script/grupo/Create";
    }

    public boolean puedeVer(Grupo item) {
        return item != null;
    }

    public boolean puedeEditar(Grupo item) {
        return (puedeVer(item) && item.isEditable());
    }

    public String create() {
        try {
            current.setEditable(true);
            List<Categoria> catsSelected = new ArrayList<Categoria>(getSelectedCategoriasNodes().length);
            for (TreeNode treeNode : getSelectedCategoriasNodes()) {
                catsSelected.add((Categoria) treeNode.getData());
            }

            current.setCategoriaList(catsSelected);
            current.setUsuarioList(getUsuariosDualListModel().getTarget());
            current.setProductoList(getProductoDualListModel().getTarget());

            for (Producto productoListProducto : current.getProductoList()) {
                productoListProducto.getGrupoList().add(current);
            }
            for (Categoria categoriaListCategoria : current.getCategoriaList()) {
                categoriaListCategoria.getGrupoList().add(current);
            }
            for (Usuario usuarioListUsuario : current.getUsuarioList()) {
                usuarioListUsuario.getGrupoList().add(current);
            }

            getJpaController().merge(current);

            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("GrupoCreated"));
            return prepareCreate();
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit(Grupo item) throws Exception {
        setSelected(item);
        setUsuariosDualListModel(new DualListModel<Usuario>((List<Usuario>) getJpaController().findAll(Usuario.class), current.getUsuarioList()));
        for (Usuario user : current.getUsuarioList()) {
            if (getUsuariosDualListModel().getSource().contains(user)) {
                getUsuariosDualListModel().getSource().remove(user);
            }
        }
        setProductoDualListModel(new DualListModel<Producto>((List<Producto>) getJpaController().findAll(Producto.class), current.getProductoList()));
        for (Producto producto : current.getProductoList()) {
            if (getProductoDualListModel().getSource().contains(producto)) {
                getProductoDualListModel().getSource().remove(producto);
            }
        }

        return "/script/grupo/Edit";
    }

    public String prepareView(Grupo item) throws Exception {
        selectItem(item);
        if (getSelected() == null) {
            JsfUtil.addSuccessMessage("Se requiere que seleccione un grupo para visualizar.");
            return "";
        }
        return "/script/grupo/View";
    }

    public void selectItem(Grupo item) {
        //System.out.println("selectItem="+item.getIdGrupo());
        current = item;
    }

    public String prepareEdit() {
        if (getSelected() == null) {
            JsfUtil.addSuccessMessage("Se requiere que seleccione una fila para editar.");
            return null;
        }
        setUsuariosDualListModel(new DualListModel<Usuario>((List<Usuario>) getJpaController().findAll(Usuario.class), current.getUsuarioList()));
        for (Usuario user : current.getUsuarioList()) {
            if (getUsuariosDualListModel().getSource().contains(user)) {
                getUsuariosDualListModel().getSource().remove(user);
            }
        }
        setProductoDualListModel(new DualListModel<Producto>((List<Producto>) getJpaController().findAll(Producto.class), current.getProductoList()));
        for (Producto producto : current.getProductoList()) {
            if (getProductoDualListModel().getSource().contains(producto)) {
                getProductoDualListModel().getSource().remove(producto);
            }
        }
        try {
            selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        } catch (Exception e) {
            //ignore
        }
        //setSelectedCategoriasNodes(generarCatTreeNodesSeleccionados(current.getCategoriaList(), getTreeNodeCategorias()));

        return "/script/grupo/Edit";
    }

//    private TreeNode[] generarCatTreeNodesSeleccionados(List<Categoria> categorias, TreeNode arbol) {
//        LinkedList<TreeNode> lista = new LinkedList<TreeNode>();
//        generarCatTreeNodesSeleccionados(categorias, arbol, lista);
//        TreeNode[] arreglo = new TreeNode[lista.size()];
//        for (int i = 0; i < lista.size(); i++) {
//            lista.get(i).setSelected(true);
//            arreglo[i] = lista.get(i);
//        }
//        return arreglo;
//    }
    private void generarCatTreeNodesSeleccionados(List<Categoria> categorias, TreeNode nodo, List<TreeNode> lista) {
        if (nodo != null) {
            if (nodo.getData() instanceof Categoria) {
                if (categorias.contains((Categoria) nodo.getData())) {
                    lista.add(nodo);
                }
            }
            for (TreeNode treeNode : nodo.getChildren()) {
                generarCatTreeNodesSeleccionados(categorias, treeNode, lista);
            }
        }
    }

    public TreeNode getTreeNodeCategorias() {

//        if (treeNodeCategorias != null) {
//            return treeNodeCategorias;
//        }
        CategoriaDataModel categoriaDataModel = createCategoriasDataModel();

        if (categoriaDataModel != null) {
            //System.out.println("se reconstruye el arbol");
            Iterator it = categoriaDataModel.iterator();
            treeNodeCategorias = new DefaultTreeNode("Categoria", null);
            treeNodeCategorias.setExpanded(true);
            while (it.hasNext()) {
                Categoria cat = (Categoria) it.next();
                verificaSiCatEsOrdenMayor(cat.getOrden());
                if (cat.getIdCategoriaPadre() == null) {
                    if (cat.getCategoriaList().isEmpty()) {
                        TreeNode subCategorias = new DefaultTreeNode(cat, treeNodeCategorias);
                        subCategorias.setExpanded(true);
                        if (current.getCategoriaList() != null) {
                            subCategorias.setSelected(current.getCategoriaList().contains(cat));
                        }
                    } else {
                        TreeNode subCategorias = new DefaultTreeNode(cat, treeNodeCategorias);
                        subCategorias.setExpanded(true);
                        if (current.getCategoriaList() != null) {
                            subCategorias.setSelected(current.getCategoriaList().contains(cat));
                        }
                        crearArbol(cat, subCategorias);
                    }
                }
            }
        } else {
            treeNodeCategorias = new DefaultTreeNode("Categoria", null);
        }

        return treeNodeCategorias;
    }

    /**
     * Crea el arbol de categoria desde un Padre bastardo.
     *
     * @param categoria Categoria Padre
     * @param subCategorias TreeNode Padre
     * @return
     */
    private void crearArbol(Categoria categoria, TreeNode subCategorias) {

        List<Categoria> cats = (List) categoria.getCategoriaList();
        Collections.sort(cats, new Comparator<Categoria>() {
            @Override
            public int compare(Categoria o1, Categoria o2) {
                return o1.getOrden() - o2.getOrden();
            }
        });
        Iterator it = cats.iterator();
        while (it.hasNext()) {
            Categoria cat = (Categoria) it.next();
            verificaSiCatEsOrdenMayor(cat.getOrden());
            if (cat.getCategoriaList().isEmpty()) {
                TreeNode cate = new DefaultTreeNode(cat, subCategorias);
                cate.setExpanded(true);
                if (current.getCategoriaList() != null) {
                    cate.setSelected(current.getCategoriaList().contains(cat));
                }
            } else {
                TreeNode cate = new DefaultTreeNode(cat, subCategorias);
                cate.setExpanded(true);
                if (current.getCategoriaList() != null) {
                    cate.setSelected(current.getCategoriaList().contains(cat));
                }
                crearArbol(cat, cate);
            }

        }
    }

    private void verificaSiCatEsOrdenMayor(int orden) {
        if (orden > categoriaOrdenMayor) {
            categoriaOrdenMayor = orden;
        }
    }

    private CategoriaDataModel createCategoriasDataModel() {
        List<Categoria> lista = (List<Categoria>) getJpaController().findAll(Categoria.class);
        ListDataModel categoriasListDataModel = new ListDataModel(lista);

        Iterator iter = categoriasListDataModel.iterator();
        List<Categoria> listOfCategoria = new ArrayList<Categoria>();
        while (iter.hasNext()) {
            listOfCategoria.add((Categoria) iter.next());
        }
        Collections.sort(listOfCategoria, new Comparator<Categoria>() {
            @Override
            public int compare(Categoria o1, Categoria o2) {
                return o1.getOrden() - o2.getOrden();
            }
        });

        return new CategoriaDataModel(listOfCategoria);
    }

    public String update() {
        try {
            List<Categoria> catsSelected = new ArrayList<Categoria>(getSelectedCategoriasNodes().length);
            for (TreeNode treeNode : getSelectedCategoriasNodes()) {
                catsSelected.add((Categoria) treeNode.getData());
            }
            current.setCategoriaList(catsSelected);
            current.setUsuarioList(getUsuariosDualListModel().getTarget());
            current.setProductoList(getProductoDualListModel().getTarget());

            getJpaController().merge(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("GrupoUpdated"));
            return "/script/grupo/Edit";
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        if (current == null) {
            JsfUtil.addErrorMessage("Debe seleccionar un grupo.");
            return null;
        }
//        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        if (performDestroy()) {
            recreateModel();
            return "/script/grupo/List";
        }
        return null;
    }

    private boolean performDestroy() {
        try {
            if (current.getUsuarioList().size() > 0) {
                JsfUtil.addErrorMessage("No se puede eliminar, el grupo aun tiene usuarios asociados");
                return false;
            }
            getJpaController().remove(Grupo.class, current.getIdGrupo());
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("GrupoDeleted"));
            return true;
        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
        return false;
    }

//    private void updateCurrentItem() {
//        int count = getJpaController().count(Grupo.class).intValue();
//        if (selectedItemIndex >= count) {
//            // selected index cannot be bigger than number of items:
//            selectedItemIndex = count - 1;
//            // go to previous page if last page disappeared:
//            if (pagination.getPageFirstItem() >= count) {
//                pagination.previousPage();
//            }
//        }
//        if (selectedItemIndex >= 0) {
//            current = (Grupo) getJpaController().queryByRange(Grupo.class, 1, selectedItemIndex).get(0);
//        }
//    }
//    public DataModel getItems() {
//        if (items == null) {
//            items = getPagination().createPageDataModel();
//        }
//        Iterator iter = items.iterator();
//        List<Grupo> listOfGrupo = new ArrayList<Grupo>();
//        while (iter.hasNext()) {
//            listOfGrupo.add((Grupo) iter.next());
//        }
//
//        return new GrupoDataModel(listOfGrupo);
//    }
//    private void recreateModel() {
//        items = null;
//    }
//
//    public String next() {
//        getPagination().nextPage();
//        recreateModel();
//        return "/script/grupo/List";
//    }
//
//    public String previous() {
//        getPagination().previousPage();
//        recreateModel();
//        return "/script/grupo/List";
//    }
//
//    public String last() {
//        getPagination().lastPage();
//        recreateModel();
//        return "/script/grupo/List";
//    }
//
//    public String first() {
//        getPagination().firstPage();
//        recreateModel();
//        return "/script/grupo/List";
//    }
//    public SelectItem[] getItemsAvailableSelectMany() {
//        return JsfUtil.getSelectItems(getJpaController().getGrupoFindAll(), false);
//    }
//
//    public SelectItem[] getItemsAvailableSelectOne() {
//        return JsfUtil.getSelectItems(getJpaController().getGrupoFindAll(), true);
//    }
    /**
     * @return the usuariosDualListModel
     */
    public DualListModel<Usuario> getUsuariosDualListModel() {
        return usuariosDualListModel;
    }

    /**
     * @param usuariosDualListModel the usuariosDualListModel to set
     */
    public void setUsuariosDualListModel(DualListModel<Usuario> usuariosDualListModel) {
        this.usuariosDualListModel = usuariosDualListModel;
    }

    @Override
    public Class getDataModelImplementationClass() {
        return GrupoDataModel.class;
    }

    /**
     * @return the productoDualListModel
     */
    public DualListModel<Producto> getProductoDualListModel() {
        return productoDualListModel;
    }

    /**
     * @param productoDualListModel the productoDualListModel to set
     */
    public void setProductoDualListModel(DualListModel<Producto> productoDualListModel) {
        this.productoDualListModel = productoDualListModel;
    }

    @FacesConverter(forClass = Grupo.class)
    public static class GrupoControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            GrupoController controller = (GrupoController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "grupoController");
            return controller.getJpaController().find(Grupo.class, getKey(value));
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
            if (object instanceof Grupo) {
                Grupo o = (Grupo) object;
                return getStringKey(o.getIdGrupo());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + GrupoController.class.getName());
            }
        }
    }
}

class GrupoDataModel extends ListDataModel<Grupo> implements SelectableDataModel<Grupo> {

    public GrupoDataModel() {
        //nothing
    }

    public GrupoDataModel(List<Grupo> data) {
        super(data);
    }

    @Override
    public Grupo getRowData(String rowKey) {
        List<Grupo> listOfGrupo = (List<Grupo>) getWrappedData();

        for (Grupo obj : listOfGrupo) {
            if (obj.getIdGrupo().equals(rowKey)) {
                return obj;
            }
        }

        return null;
    }

    @Override
    public Object getRowKey(Grupo classname) {
        return classname.getIdGrupo();
    }
}
