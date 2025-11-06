package com.example;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        resp.setContentType("text/html;charset=UTF-8");
        
        try (PrintWriter out = resp.getWriter()) {
            String hostname = InetAddress.getLocalHost().getHostName();
            
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Test Application</title>");
            out.println("<style>");
            out.println("body { font-family: Arial; max-width: 800px; margin: 50px auto; padding: 20px; }");
            out.println(".success { background: #4CAF50; color: white; padding: 20px; border-radius: 8px; }");
            out.println(".info { background: #f5f5f5; padding: 15px; margin: 20px 0; border-left: 4px solid #2196F3; }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='success'>");
            out.println("<h1>Déploiement application java maven réussi sur Ubuntu!</h1>");
            out.println("</div>");
            out.println("<div class='info'>");
            out.println("<p><strong>Serveur:</strong> " + hostname + "</p>");
            out.println("<p><strong>Date:</strong> " + new java.util.Date() + "</p>");
            out.println("<p><strong>Java Version:</strong> " + System.getProperty("java.version") + "</p>");
            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
