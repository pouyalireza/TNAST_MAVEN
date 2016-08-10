package com.webapp.api;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/TNAtoolAPI-Webapp")
public class MainMap extends HttpServlet {
	// GET
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		request.getRequestDispatcher(/*"/WEB-INF/views/index.jsp"*/"/WEB-INF/views/GeoCountiesReport.html?&dbindex=3&popYear=2010").forward(request, response);
		
	}
	
	// POST
}
