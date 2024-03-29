/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.webapputils;

import com.itcs.helpdesk.jsfcontrollers.util.JsfUtil;
import com.itcs.helpdesk.persistence.entities.Etiqueta;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.json.JSONArray;

/**
 *
 * @author jonathan
 */
public class TagListServlet extends HttpServlet {

    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "helpdeskPU")
    private EntityManagerFactory emf = null;
    private JPAServiceFacade jpaController = null;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String schema = request.getParameter("tenant");
       
            if (!StringUtils.isEmpty(schema)) {
               
                /* TODO output your page here. You may use following sample code. */
                String q = request.getParameter("q");
                if (q != null && !StringUtils.isEmpty(q)) {
                    out.println(getTagJSonList(q));
                } else {
                    out.println(getTagJSonList(schema));
                }
                System.out.println("getTagJSonList(" + q + ")");
            }
        } finally {
            out.close();
        }
    }

    public String getTagJSonList(String schema, String pattern) {
        try {
            JSONArray list = new JSONArray((List<Etiqueta>) getJpaController(schema).findEtiquetasLike(pattern, JsfUtil.getUserSessionBean().getCurrent().getIdUsuario()));
            return list.toString();
        } catch (NullPointerException npe) {
            return new JSONArray(Collections.EMPTY_LIST).toString();
        }
    }

    public String getTagJSonList(String schema) {
        JSONArray list = new JSONArray((List<Etiqueta>) getJpaController(schema).findAll(Etiqueta.class));
        return list.toString();
    }

    public JPAServiceFacade getJpaController(String schema) {
        if (jpaController == null) {
            jpaController = new JPAServiceFacade(utx, emf, schema);//TODO Change schema dynamic
        }
        return jpaController;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
