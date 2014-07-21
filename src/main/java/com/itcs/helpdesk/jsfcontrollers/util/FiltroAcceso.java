/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.jsfcontrollers.util;

import com.itcs.helpdesk.persistence.entities.Rol;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.persistence.entityenums.EnumFunciones;
import com.itcs.helpdesk.persistence.entityenums.EnumGrupos;
import com.itcs.helpdesk.persistence.entityenums.EnumRoles;
import com.itcs.helpdesk.persistence.entityenums.EnumUsuariosBase;
import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

/**
 *
 * @author danilomoya TODO DELETE THIS FUKIN CLASS!
 */
@ManagedBean(name = "filtroAcceso")
@SessionScoped
public class FiltroAcceso implements Serializable {

    @ManagedProperty(value = "#{UserSessionBean}")
    private UserSessionBean userSessionBean;

    private Boolean administradorDelSistema = null;
    private Boolean esUsuarioSistema = null;
    private Boolean perteneceAGrupoSistema = null;
    private Boolean accesoAFuncionAgregarCaso = null;
    private Boolean accesoAFuncionAdministrarVistas = null;
    private Boolean accesoAFuncionSupervision = null;
    private Boolean accesoAFuncionEditarAjustes = null;
 
    private Boolean accesoA_EDITAR_CUALQUIER_CASO;
    private Boolean accesoA_RESPONDER_CUALQUIER_CASO;
    private Boolean accesoA_FILTRO_CASOS;
    private Boolean accesoA_ASIGNAR_TRANSFERIR_CASO;
    private Boolean accesoA_CAMBIAR_CATEGORIA_CASO;
    private Boolean accesoA_ELIMINAR_CASO;
    private Boolean accesoA_EDITAR_CASO;

    public FiltroAcceso() {
    }

    public Boolean isAdministradorDelSistema() {
        if (administradorDelSistema == null) {
            for (Rol rol : userSessionBean.getCurrent().getRolList()) {
                if (rol.equals(EnumRoles.ADMINISTRADOR.getRol())) {
                    administradorDelSistema = Boolean.TRUE;
                    return administradorDelSistema;
                }
            }
            administradorDelSistema = Boolean.FALSE;

        }

        return administradorDelSistema;

    }

    public boolean esUsuarioSistema() {
        if (esUsuarioSistema == null) {
            Usuario user = userSessionBean.getCurrent();
            if (user != null && user.equals(EnumUsuariosBase.SISTEMA.getUsuario())) {
                esUsuarioSistema = Boolean.TRUE;
                return esUsuarioSistema;
            }
            esUsuarioSistema = Boolean.FALSE;
        }

        return esUsuarioSistema;

    }

    private boolean verificaAccesoAFuncion(EnumFunciones funcion) {
        Usuario user = userSessionBean.getCurrent();
        if (user == null) {
            return false;
        }
        if (user.getRolList() != null) {
            for (Rol rol : user.getRolList()) {
                if (rol.getFuncionList().contains(funcion.getFuncion())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean verificaAccesoAGrupo(EnumGrupos grupo) {
        Usuario user = userSessionBean.getCurrent();
        return user != null && user.getGrupoList() != null && user.getGrupoList().contains(grupo.getGrupo());
    }

    public boolean perteneceAGrupoSistema() {
        if (perteneceAGrupoSistema == null) {
            perteneceAGrupoSistema = verificaAccesoAGrupo(EnumGrupos.GRUPO_SISTEMA);
        }
        return perteneceAGrupoSistema;
    }

    public boolean verificarAccesoAFuncionAdministrarVistas() {
        if (accesoAFuncionAdministrarVistas == null) {
            if (esUsuarioSistema()) {
                accesoAFuncionAdministrarVistas = Boolean.TRUE;
                return accesoAFuncionAdministrarVistas;
            }
            accesoAFuncionAdministrarVistas = verificaAccesoAFuncion(EnumFunciones.ADMINISTRAR_VISTAS);
        }
        return accesoAFuncionAdministrarVistas;
    }

    public boolean verificarAccesoAFuncionSupervision() {
        if (accesoAFuncionSupervision == null) {
            if (esUsuarioSistema()) {
                accesoAFuncionSupervision = Boolean.TRUE;
                return accesoAFuncionSupervision;
            }
            accesoAFuncionSupervision = verificaAccesoAFuncion(EnumFunciones.SUPERVISOR);
        }
        return accesoAFuncionSupervision;
    }

    public boolean verificarAccesoAFuncionEditarAjustes() {

        if (accesoAFuncionEditarAjustes == null) {
            if (esUsuarioSistema()) {
                accesoAFuncionEditarAjustes = Boolean.TRUE;
                return accesoAFuncionEditarAjustes;
            }
            accesoAFuncionEditarAjustes = verificaAccesoAFuncion(EnumFunciones.EDITAR_AJUSTES);
        }
        return accesoAFuncionEditarAjustes;

    }

    public boolean verificarAccesoAFuncionAgregarCaso() {
        if (accesoAFuncionAgregarCaso == null) {
            accesoAFuncionAgregarCaso = verificaAccesoAFuncion(EnumFunciones.AGREGAR_CASO);
        }
        return accesoAFuncionAgregarCaso;
    }

    public boolean verificarAccesoAFuncionEditarCaso() {
        if (accesoA_EDITAR_CASO == null) {
            accesoA_EDITAR_CASO = verificaAccesoAFuncion(EnumFunciones.EDITAR_CASO);
        }
        return accesoA_EDITAR_CASO;
    }

    public boolean verificarAccesoAFuncionEliminarCaso() {
        if (accesoA_ELIMINAR_CASO == null) {
            accesoA_ELIMINAR_CASO = verificaAccesoAFuncion(EnumFunciones.ELIMINAR_CASO);
        }
        return accesoA_ELIMINAR_CASO;
    }

    public boolean verificarAccesoAFuncionCambiarCategoriaCaso() {
        if (accesoA_CAMBIAR_CATEGORIA_CASO == null) {
            accesoA_CAMBIAR_CATEGORIA_CASO = verificaAccesoAFuncion(EnumFunciones.CAMBIAR_CATEGORIA_CASO);
        }
        return accesoA_CAMBIAR_CATEGORIA_CASO;
    }

    public boolean verificarAccesoAFuncionAsignarTransferirCaso() {
        if (accesoA_ASIGNAR_TRANSFERIR_CASO == null) {
            accesoA_ASIGNAR_TRANSFERIR_CASO = verificaAccesoAFuncion(EnumFunciones.ASIGNAR_TRANSFERIR_CASO);
        }
        return accesoA_ASIGNAR_TRANSFERIR_CASO;
    }

    public boolean verificarAccesoAFuncionFiltroCasos() {
        if (accesoA_FILTRO_CASOS == null) {
            accesoA_FILTRO_CASOS = verificaAccesoAFuncion(EnumFunciones.FILTRO_CASOS);
        }
        return accesoA_FILTRO_CASOS;
    }

    public boolean verificarAccesoAFuncionResponderCualquierCaso() {
        if (accesoA_RESPONDER_CUALQUIER_CASO == null) {
            accesoA_RESPONDER_CUALQUIER_CASO = verificaAccesoAFuncion(EnumFunciones.RESPONDER_CUALQUIER_CASO);
        }
        return accesoA_RESPONDER_CUALQUIER_CASO;
    }

    public boolean verificarAccesoAFuncionEditarCualquierCaso() {
        if (accesoA_EDITAR_CUALQUIER_CASO == null) {
            accesoA_EDITAR_CUALQUIER_CASO = verificaAccesoAFuncion(EnumFunciones.EDITAR_CUALQUIER_CASO);
        }
        return accesoA_EDITAR_CUALQUIER_CASO;
    }

    /**
     * @param userSessionBean the userSessionBean to set
     */
    public void setUserSessionBean(UserSessionBean userSessionBean) {
        this.userSessionBean = userSessionBean;
    }
}
