package fedora.server.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import fedora.common.http.HttpInputStream;
import fedora.common.http.WebClient;
import fedora.server.Server;
import fedora.server.errors.GeneralException;
import fedora.server.errors.InitializationException;
import fedora.server.errors.servletExceptionExtensions.InternalError500Exception;

/**
 * <p><b>Title: </b>AxisWSDLFilterServlet.java</p>
 * <p><b>Description: </b>Implements a simple proxy that allows unrestricted access
 * to the API-A and API-M WSDL generated by Fedora using Apache Axis. The servlet
 * mappings for this servlet are unrestricted in the Fedora web.xml file. This
 * servlet uses the information contained in the fedora.fcfg configuration file 
 * to authenticate and provide secure SSL connections (if necessary) to the
 * underlying Fedora server and then return the generated WSDL. This proxy enables
 * unrestricted access to the generated WSDL regardless of the security configuration
 * being used by the fedora server.
 * <ol>
 *
 * @author rlw@virginia.edu
 * @version $Id$
 */
public class AxisWSDLFilterServlet extends HttpServlet
{
	
	  private static Server s_server = null;
	  private static String fedoraServerHost = null;
	  private static String fedoraServerPort = null;  
	  private static String fedoraAdminUsername = null;
	  private static String fedoraAdminPassword = null;

  /**
   * <p>Process Fedora Access Request. Parse and validate the servlet input
   * parameters and then execute the specified request.</p>
   *
   * @param request  The servlet request.
   * @param response servlet The servlet response.
   * @throws ServletException If an error occurs that effects the servlet's
   *         basic operation.
   * @throws IOException If an error occurrs with an input or output operation.
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
	  String axisWSDLURL = null;
	  String requestURL = request.getRequestURL().toString();
	  
	  try {
		  
		  // The fedora server uses SSL auto-forwarding in its web.xml file so it is not necessary
		  // to specify the specific protocol and port in the request URL. If the Fedora server 
		  // is configured to use SSL for either API-A or API-M, then the incoming request will 
		  // be auto-forwarded to the appropriate protocol and SSL port.
		  
		  if (requestURL.indexOf("fedora/access/wsdl") != -1) {
		  	  axisWSDLURL = "http://" + fedoraServerHost + ":" + fedoraServerPort + "/fedora/services/access?wsdl";
		  } else if (requestURL.indexOf("fedora/management/wsdl") != -1) {
		  	  axisWSDLURL = "http://" + fedoraServerHost + ":" + fedoraServerPort + "/fedora/services/management?wsdl";
		  }
		  
		  // Get the Axis-generated WSDL from Fedora server (internally authenticating as necessary)
		  // and write the contents back out to the response.
		  String axisGeneratedWSDL = get(axisWSDLURL); 
		  response.setContentType("text/xml; charset=\"UTF-8\"");
	      OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream(),"UTF-8");
	      out.write(axisGeneratedWSDL);
	      out.flush();
	      
	    } catch (Throwable th) {
	    	th.printStackTrace();
	    	throw new InternalError500Exception("", th, request, th.getMessage(), "", new String[0]);
	    }
	  
  }

  /**
   * <p>Get the contents of the specified URL</p>
   * 
   * @param url The URL string.
   * @return The contents of the URL.
   * @throws GeneralException If unable to retrieve the contents of the URL.
   */
  private String get(String url) throws GeneralException {
	  
	  HttpInputStream response = null;
	  try {
		  WebClient client = new WebClient();
		  response = client.get(url, true, fedoraAdminUsername, fedoraAdminPassword);
		  
          BufferedInputStream bis = new BufferedInputStream(response); 
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          int len=0;
          while ( (len = bis.read()) != -1) {
              baos.write(len);
          }
          return baos.toString();
          
	  } catch (Exception e) {
		  System.err.println("*** Unable to retrieve contents of URL: "+url+" Reason: "+e.getMessage());
		  throw new GeneralException("Unable to retrieve contents of URL:  " + url, e);
	  } finally {
		  try {
			  response.close(); 
		  } catch (Exception e) {
			  System.err.println("Can't close InputStream: " + e.getMessage());
		  }
	  }
}
  
  /**
   * <p>treat a HTTP POST request just like a GET request.</p>
   *
   * @param request The servet request.
   * @param response The servlet response.
   * @throws ServletException If thrown by <code>doGet</code>.
   * @throws IOException If thrown by <code>doGet</code>.
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    doGet(request, response);
  }

  /**
   * <p>Initialize servlet.</p>
   *
   * @throws ServletException If the servet cannot be initialized.
   */
  public void init() throws ServletException
  {
      try
      {
          s_server=Server.getInstance(new File(System.getProperty("fedora.home")), false);
          fedoraServerHost = s_server.getParameter("fedoraServerHost");
          fedoraServerPort = s_server.getParameter("fedoraServerPort");
          fedoraAdminUsername = s_server.getParameter("adminUsername");
          fedoraAdminPassword = s_server.getParameter("adminPassword");
          } catch (InitializationException ie)
          {
              throw new ServletException("Unable to get Fedora Server instance."
                  + ie.getMessage());
          }

  }

  /**
   * <p>Cleans up servlet resources.</p>
   */
  public void destroy()
  {}

  
}
