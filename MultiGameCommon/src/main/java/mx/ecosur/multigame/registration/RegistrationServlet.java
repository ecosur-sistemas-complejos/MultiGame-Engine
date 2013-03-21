/*
 * Copyright (C) 2013 ECOSUR, Andrew Waterman
 *
 * Licensed under the Academic Free License v. 3.0.
 * http://www.opensource.org/licenses/afl-3.0.php
 */

package mx.ecosur.multigame.registration;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;

/**
 * The RegistrationServlet creates Users with the MultiGame role for
 * use in the project.
 */
public class RegistrationServlet extends HttpServlet {

    /* Static method for grabbing connections from DataSource */
    private static Connection getConnection() {
        /* Explanation: Using JDBC as JBOSS 6.1 can't seem to insert @EJB resources correctly into the servlet */
        Connection con = null;
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:/MySQLDS");
            con = ds.getConnection();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return con;
    }

    /* Command interface for Registration Commands */
    interface ServletCommand {
        void execute() throws ServletException;
    }

    /* Legal commands are listed in the CommandField enum */
    enum CommandField {
        CREATE, SEND, UPDATE
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException
    {
        if (request.getParameter(CommandField.CREATE.name()) != null) {
            CreateUserServletCommand create = new CreateUserServletCommand(request, response);
            create.execute();
        } else if (request.getParameter(CommandField.SEND.name()) != null) {
            SendUserServletCommand send = new SendUserServletCommand(request, response);
            send.execute();
        } else if (request.getParameter(CommandField.UPDATE.name()) != null) {
            UpdateUserCommand update = new UpdateUserCommand(request, response);
            update.execute();
        }
    }


    class CreateUserServletCommand implements ServletCommand {

        HttpServletRequest request;

        HttpServletResponse response;

        CreateUserServletCommand(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public void execute() throws ServletException {
            Connection con = null;
            String username  = request.getParameter("username");
            String password  = request.getParameter("password1");
            String passmatch = request.getParameter("password2");
            String firstname = request.getParameter("firstname");
            String lastname  = request.getParameter("lastname");
            String email     = request.getParameter("email");

            if (!password.equals(passmatch))
                throw new ServletException ("Passwords don't match!");

            try {
                con = getConnection();
                /* Using prepared statements to avoid SQL injection issues */
                PreparedStatement userInsert = con.prepareStatement("insert into user (username, password, firstname, " +
                        "lastname, email) values (?, md5(?), ?, ?, ?)");
                PreparedStatement roleInsert = con.prepareStatement("insert into role (username, name) values (?, " +
                        "'MultiGame')");
                userInsert.setString(1,username);
                userInsert.setString(2,password);
                userInsert.setString(3,firstname);
                userInsert.setString(4,lastname);
                userInsert.setString(5,email);
                roleInsert.setString(1,username);
                userInsert.execute();
                roleInsert.execute();
                con.close();

                /* cleanup */
                if (request.getSession().getAttribute("sqlerror") != null) {
                    request.getSession().removeAttribute("sqlerror");
                }

                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/login.jsp");
                try {
                    dispatcher.forward(request,response);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new ServletException(e);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                if (con != null) try { con.close(); } catch (Exception exception) {};
                request.getSession().setAttribute("sqlerror", e.getLocalizedMessage());
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/register.jsp");
                try {
                    dispatcher.forward(request,response);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    throw new ServletException(ex);
                }
            }
        }
    }

    class SendUserServletCommand implements ServletCommand {

        HttpServletRequest request;

        HttpServletResponse response;

        SendUserServletCommand(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }


        @Override
        public void execute() throws ServletException {
            Connection con = null;
            try {
                con = getConnection();
                String j_username = request.getParameter("j_username");
                if (j_username != null) {
                   /* Check for the user in the db */
                   PreparedStatement pstmt = con.prepareStatement("select * from user where username = ?");
                   PreparedStatement uidUpdate = con.prepareStatement("update user set uid= ? where id = ?");
                   pstmt.setString(1,j_username);
                   ResultSet rs = pstmt.executeQuery();
                   if (rs.next()) {
                       int idCol = rs.findColumn("id");
                       int uidCol = rs.findColumn("uid");
                       int emailCol = rs.findColumn("email");
                       String guard = rs.getString(uidCol);
                       if (guard == null) {
                           String email = rs.getString(emailCol);
                           int id = rs.getInt(idCol);
                           UUID uid = UUID.randomUUID();
                           uidUpdate.setString(1, uid.toString());
                           uidUpdate.setInt(2, id);
                           uidUpdate.execute();
                           /* Mail uid to user */
                           mailUID(email, uid);
                       }
                   }
                   rs.close();
                }
                con.close();

                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/login.jsp");
                dispatcher.forward(request,response);

            } catch (IOException e) {
                e.printStackTrace();
                if (con != null) { try { con.close(); } catch (Exception ex) { }};
                throw new ServletException(e);
            } catch (SQLException e) {
                e.printStackTrace();
                if (con != null) { try { con.close(); } catch (Exception ex) { }};
                throw new ServletException(e);
            } catch (MessagingException e) {
                if (con != null) { try { con.close(); } catch (Exception ex) { }};
                e.printStackTrace();
            }
        }

        private void mailUID(String email, UUID uid) throws ServletException, MessagingException {
            try {
                InitialContext ictx = new InitialContext();
                Session mailSession = (Session) ictx.lookup("java:/Mail");
                MimeMessage message = new MimeMessage(mailSession);
                message.setSubject("Password/Contraseña");
                message.setRecipients(javax.mail.Message.RecipientType.TO,
                        javax.mail.internet.InternetAddress.parse(email, false));
                message.setText("http://localhost:8080/multigame/update.jsp?uid=" + uid.toString());
                message.saveChanges();
                Transport t = mailSession.getTransport("smtp");
                t.connect();
                t.sendMessage(message, message.getAllRecipients());
                log("Message sent: " + message.toString());
                t.close();
            } catch (NamingException e) {
                e.printStackTrace();
                throw new ServletException(e);
            }
        }
    }

    class UpdateUserCommand implements ServletCommand {

        HttpServletRequest request;

        HttpServletResponse response;

        UpdateUserCommand(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public void execute() throws ServletException {
            Connection con = null;
            String uid = request.getParameter("uid");
            String password  = request.getParameter("password1");
            String passmatch = request.getParameter("password2");
            if (!password.equals(passmatch))
                throw new ServletException("Passwords don't match! " + password + " != " + passmatch);

            log("uid=" + uid);
            log("password=" + password);

            if (uid != null && password != null) {
                try {
                    con = getConnection();
                    PreparedStatement update = con.prepareStatement("update user set password = md5(?) where uid = ?");
                    PreparedStatement delete = con.prepareStatement("update user set uid = null where uid = ?");
                    update.setString(1,password);
                    update.setString(2,uid);
                    delete.setString(1,uid);
                    update.execute();
                    delete.execute();
                    con.close();

                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/login.jsp");
                    dispatcher.forward(request,response);


                } catch (SQLException e) {
                    e.printStackTrace();
                    if (con != null) { try { con.close(); } catch (SQLException sqle) {}};
                    throw new ServletException (e);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new ServletException (e);
                }
            } else {
                log("Error! No UID in UpdateUser request.");
                response.setStatus(500);
            }
        }
    }
}
