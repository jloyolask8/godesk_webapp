package com.itcs.helpdesk.jsfcontrollers.util;

import com.itcs.helpdesk.util.ApplicationConfig;
import com.itcs.helpdesk.util.Log;
import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import javax.servlet.http.*;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Danilo
 */
public class LoginFilter implements Filter {

    private FilterConfig filterConfig = null;

    public LoginFilter() {
    }

    private String getHostSubDomain(String url) {
        try {
            URL url_ = new URL(url);
            String host = url_.getHost();
            if (host != null && !host.equalsIgnoreCase("localhost")) {
                return host.substring(0, host.indexOf(".godesk.cl"));
            } else {
                return host;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession();
        String requestedPage = req.getPathTranslated();//getRequestURL().toString();

        UserSessionBean userSessionBean = (UserSessionBean) session.getAttribute("UserSessionBean");

        System.out.println("userSessionBean:" + userSessionBean);

        System.out.println("Filtering Request:" + req.getRequestURL().toString());

        String tenantId = null;

        if (userSessionBean != null && userSessionBean.getTenantId() != null) {
            tenantId = userSessionBean.getTenantId();
        } else {
            tenantId = getHostSubDomain(req.getRequestURL().toString());
        }

        System.out.println("tenantId:" + tenantId);

        try {

            if (!StringUtils.isEmpty(tenantId)) {

                if (ApplicationConfig.getInstance(tenantId) != null) {

                    if (requestedPage.endsWith(".xhtml")) {
                        if (requestedPage.endsWith("login.xhtml")) {
                            if (userSessionBean != null) {//TODO replace this check for a subdomain token taken from the URL and put in a cookie before any request.
                                if (userSessionBean.isValidatedSession()) {
                                    res.sendRedirect(req.getContextPath() + "/faces/script/index.xhtml");
                                    return;
                                }
                            }

                        } else if (requestedPage.endsWith("context.xhtml")) {
                            //if there is a session then go to index.
                            if (userSessionBean != null) {
                                if (userSessionBean.isValidatedSession()) {
                                    res.sendRedirect(req.getContextPath() + "/faces/script/index.xhtml");
                                    return;
                                }
                            }

                        } else if (requestedPage.endsWith("signup.xhtml")) {
                            //let him pass
                        } else {
                            //filter resource
                            if (userSessionBean == null) {
                                res.sendRedirect(req.getContextPath() + "/faces/script/login.xhtml");
                                return;
                            } else {

                                if (!userSessionBean.isValidatedSession()) {
                                    res.sendRedirect(req.getContextPath() + "/faces/script/login.xhtml");
                                    return;
                                }
                            }
                        }
                    }
                } else {
                    if (!requestedPage.endsWith("context_error.xhtml")) {
                        res.sendRedirect(req.getContextPath() + "/faces/script/context_error.xhtml");
                        return;

                    }

                }

            } else {
                if (!requestedPage.endsWith("context.xhtml")) {
                    res.sendRedirect(req.getContextPath() + "/faces/script/context.xhtml");
                    return;

                }

            }

        } catch (Exception e) {
            Log.createLogger(this.getClass().getName()).log(Level.SEVERE, "Error en login filtering", e);
            return;
        }
        chain.doFilter(request, response);

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
//        if (filterConfig != null) {
//
//        }
    }

    @Override
    public void destroy() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }
}
