package com.sap.hcp.cf.logging.servlet.filter;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTestServlet extends HttpServlet {

    public static final String LOG_MESSAGE = "request received";

    private static final long serialVersionUID = -4594560302550583354L;
    private static final Logger LOG = LoggerFactory.getLogger(LoggingTestServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.info(LOG_MESSAGE);
    }

}
