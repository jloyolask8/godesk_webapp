/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.itcs.helpdesk.webapputils;

import com.itcs.helpdesk.persistence.entities.Archivo;
import com.itcs.helpdesk.persistence.jpa.service.JPAServiceFacade;
import com.itcs.helpdesk.util.ManagerCasos;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Jonathan
 */
public class DownloadFileServlet extends HttpServlet {

    @Resource
    private UserTransaction utx = null;
    @PersistenceUnit(unitName = "helpdeskPU")
    private EntityManagerFactory emf = null;
//    private JPAServiceFacade jpaController = null;

//    private JPAServiceFacade getJpaController() {
//        if (jpaController == null) {
//            jpaController = new JPAServiceFacade(utx, emf, "public");//TODO Change schema dynamic
//        }
//        return jpaController;
//    }
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

        String fileId = request.getParameter("id");
        String schema = request.getParameter("tenant");

//        String userAgent = request.getHeader("user-agent");
//        boolean isInternetExplorer = (userAgent.indexOf("MSIE") > -1);
        try {

            System.out.println("DownloadFileServlet.schema:" + schema);

            if (!StringUtils.isEmpty(schema)) {
                JPAServiceFacade jpaController = new JPAServiceFacade(utx, emf, schema);

                if (fileId == null || "".equals(fileId)) {
                    response.sendError(404, "Debe especificar el id del archivo.");
                }
                Archivo existente = jpaController.find(Archivo.class, Long.parseLong(fileId));
                if (existente != null) {
                    System.out.println("existe archivo con id " + fileId);
                    System.out.println(existente.getContentType());
                    ServletOutputStream sot = response.getOutputStream();

                    response.setContentType(existente.getContentType());

                    String type = existente.getContentType().split("/")[0];
                    if (type.equals("image") || existente.getContentType().equals("application/pdf")) {
                        //this is needed to display preview 
                        response.setHeader("Content-Disposition", "inline; filename='" + existente.getFileName() + "'");
                    } else {
                        response.setHeader("Content-Disposition", "attachment; filename='" + existente.getFileName() + "'");
                    }

                    BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(existente.getArchivo()));
                    int i = 0;
                    byte byteArray[] = new byte[4096];
                    while ((i = bis.read(byteArray)) != -1) {
                        sot.write(byteArray, 0, i);
                    }

                    sot.flush();
                    sot.close();
                    bis.close();
                } else {
                    System.out.println("NO existe archivo con id " + fileId);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
