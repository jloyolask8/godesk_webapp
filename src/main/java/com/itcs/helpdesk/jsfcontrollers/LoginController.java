package com.itcs.helpdesk.jsfcontrollers;

import com.itcs.helpdesk.jsfcontrollers.util.ApplicationBean;
import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.jsfcontrollers.util.PaginationHelper;
import com.itcs.helpdesk.persistence.entities.Usuario;
import com.itcs.helpdesk.util.Log;
import com.itcs.helpdesk.util.UtilSecurity;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.FacesException;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean(name = "loginController")
@SessionScoped
public class LoginController extends AbstractManagedBean<Usuario> implements Serializable {

    @ManagedProperty(value = "#{applicationBean}")
    private ApplicationBean applicationBean;
//    @ManagedProperty(value = "#{casoController}")
//    private CasoController casoController;
    //private LengthValidator passwordLengthValidator = new LengthValidator();
    private String username;
    private String password;
//    private String tenantId;
    private Usuario usuario;
//    private SesionesJpaController sesionesJpaController;
    //change pass
    private String passwordCurrent;
    private String passwordNew1;
    private String passwordNew2;

    /**
     * <p>
     * Construct a new request data bean instance.</p>
     */
    public LoginController() {
        super(Usuario.class);
    }

    

    public String selectTenant() {

//        URL url_;
//        try {
//            url_ = new URL(JsfUtil.getRequest().getRequestURL().toString());
//            String host = url_.getHost();
//            if (host != null && !host.equalsIgnoreCase("localhost")) {
//                String newURL = JsfUtil.getRequest().getRequestURL().toString().replace(host, getUserSessionBean().getTenantId() + ".godesk.cl");
//                newURL = newURL.replace("context", "index");
//                redirectToURL(newURL);
//            }
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
//        }

        return "index.xhtml?faces-redirect=true";
    }

    public void loginAction() {

        if (!getUserSessionBean().isValidatedSession()) {

//            getUserSessionBean().setTenantId(tenantId);
            try {
                usuario = getJpaController().find(Usuario.class, username);
                if (usuario == null) {
                    JsfUtil.addErrorMessage("El nombre de usuario ingresado no está registrado en el sistema.");
//                    return null;
                } else if (usuario.getPass() != null) {
                    String passMD5 = UtilSecurity.getMD5(password);
//                    System.out.println("pass: "+password+" en MD5:"+passMD5);
//                    System.out.println("usuario pass: "+usuario.getPass());
                    if (usuario.getPass().equals(passMD5)) {
                        getUserSessionBean().setCurrent(usuario);
                        getUserSessionBean().setEmailCliente(null);

                        String channel = "/" + UUID.randomUUID().toString();
                        getUserSessionBean().setChannel(channel);
                        applicationBean.addChannel(usuario.getIdUsuario(), channel);

                        getCasoControllerBean().prepareCasoFilterForInbox();

//                         PushContext pushContext = PushContextFactory.getDefault().getPushContext();
//                         pushContext.push(channel, new FacesMessage("Autenticación exitósa, Cargando Datos..."));
                        //JsfUtil.addSuccessMessage("Autenticación exitosa, Cargando Datos...");
                        ConfigurableNavigationHandler nav = (ConfigurableNavigationHandler) FacesContext.getCurrentInstance().getApplication().getNavigationHandler();

                        if (isThisRequestCommingFromAMobileDevice(JsfUtil.getRequest())) {
                            nav.performNavigation("inboxMobile");//DeskTop Version

                        } else {
                            nav.performNavigation("inbox");//DeskTop Version

                        }

//                        return ResourceBundle.getBundle("/Bundle").getString("inbox");
                    } else {
                        JsfUtil.addErrorMessage("Nombre de usuario o contraseña no son válidos");
//                        return null;
                    }
                } else {
                    JsfUtil.addErrorMessage("Ud. No tiene activada su contraseña, favor comuniquese con administración para solicitar una.");
//                    return ResourceBundle.getBundle("/Bundle").getString("inbox");
                }
            } catch (NoResultException ex) {
                Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "loginAction", ex);
                JsfUtil.addErrorMessage("El nombre de usuario ingresado no está registrado en el sistema.");
//                return null;
            } catch (Exception ex) {
                Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "loginAction", ex);
                JsfUtil.addErrorMessage("No podemos atenderlo en este momento, por favor intentelo mas tarde.");
//                return null;
            }

        } else {
            HttpServletRequest request = (HttpServletRequest) JsfUtil.getRequest();
            request.getSession().invalidate();
            loginAction();
        }
    }

    public String logout_action() {
        System.out.println("logout_action");
        HttpServletRequest request = (HttpServletRequest) JsfUtil.getRequest();
        request.getSession().invalidate();
        getUserSessionBean().destroy();
        return "login";
    }

    public String changePass() {
        try {
            if (passwordCurrent != null) {
                Usuario user = getUserSessionBean().getCurrent();
                String passMD5 = UtilSecurity.getMD5(passwordCurrent);
                if (user.getPass().equals(passMD5)) {
                    //coincide con su contraseña actual! 
                    if (passwordNew1 != null && passwordNew1.equals(passwordNew2)) {
                        //So change it
                        user.setPass(UtilSecurity.getMD5(passwordNew1));
                        getJpaController().merge(user);
                        JsfUtil.addSuccessMessage("La contraseña ha sido cambiada exitósamente.");
                        executeInClient("panelChangePass.hide()");
                    } else {
                        JsfUtil.addErrorMessage("La nueva contraseña no coincide!");
                    }

                } else {
                    JsfUtil.addErrorMessage("La contraseña ingresada no coincide con su contraseña actual!");

                }
            } else {
                JsfUtil.addErrorMessage("Debe ingresar su contraseña actual.");

            }
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Ocurrió un problema con el servicio, Favor intente más tarde.");
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the passwordCurrent
     */
    public String getPasswordCurrent() {
        return passwordCurrent;
    }

    /**
     * @param passwordCurrent the passwordCurrent to set
     */
    public void setPasswordCurrent(String passwordCurrent) {
        this.passwordCurrent = passwordCurrent;
    }

    /**
     * @return the passwordNew1
     */
    public String getPasswordNew1() {
        return passwordNew1;
    }

    /**
     * @param passwordNew1 the passwordNew1 to set
     */
    public void setPasswordNew1(String passwordNew1) {
        this.passwordNew1 = passwordNew1;
    }

    /**
     * @return the passwordNew2
     */
    public String getPasswordNew2() {
        return passwordNew2;
    }

    /**
     * @param passwordNew2 the passwordNew2 to set
     */
    public void setPasswordNew2(String passwordNew2) {
        this.passwordNew2 = passwordNew2;
    }

    @Override
    public PaginationHelper getPagination() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Class getDataModelImplementationClass() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @param applicationBean the applicationBean to set
     */
    public void setApplicationBean(ApplicationBean applicationBean) {
        this.applicationBean = applicationBean;
    }

}
