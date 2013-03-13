/*
 * Copyright (C) 2013 ECOSUR, Andrew Waterman
 *
 * Licensed under the Academic Free License v. 3.0.
 * http://www.opensource.org/licenses/afl-3.0.php
 */

package mx.ecosur.multigame.registration;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The RegistrationServlet creates Users with the MultiGame role for
 * use in the project.
 */
public class RegistrationServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException
    {
        String username = request.getParameter("username");
        String password = request.getParameter("password1");
        String passmatch = request.getParameter("password2");

        if (!password.equals(passmatch))
            throw new ServletException ("Passwords don't match!");

        Connection con = null;

        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (javax.sql.DataSource) ctx.lookup("java:/MySQLDS");
            con = ds.getConnection();
            PreparedStatement userInsert = con.prepareStatement("insert into user (username, password) values (?,md5(?))");
            PreparedStatement roleInsert = con.prepareStatement("insert into role (username, name) values (?, 'MultiGame')");

            userInsert.setString(1,username);
            userInsert.setString(2,password);
            roleInsert.setString(1,username);

            userInsert.execute();
            roleInsert.execute();

            con.close();

            response.setStatus(200);
            response.sendRedirect("/multi-game");
        } catch (NamingException e) {
            e.printStackTrace();
            response.setStatus(500);

        } catch (SQLException e) {
            e.printStackTrace();
            if (con != null)
                try { con.close(); } catch (Exception exception) {};
            if (e.getErrorCode() != 1032)
                response.setStatus(500);
            else
                response.sendError(1032);
        }
    }
}
