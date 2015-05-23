/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.grizzly.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.MimeHeaders;

/**
 * {@link HttpSessionTest}
 * 
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class HttpSessionTest extends HttpServerAbstractTest {

    private static final int PORT = 12345;
    private static final String CONTEXT = "/test";
    private static final String SERVLETMAPPING = "/servlet";
    private static final String JSESSIONID_COOKIE_NAME = "JSESSIONID";
    private static String JSESSIONID_COOKIE_VALUE = "975159770778015515.OX0";
    private static final int MAX_INACTIVE_INTERVAL = 20;

    /**
     * Want to set the MaxInactiveInterval of the HttpSession in seconds via a HttpServletRequest/HttpSession.
     * @throws Exception 
     */
    public void testMaxInactiveInterval() throws Exception {
        try {
            startHttpServer(PORT);
            
            WebappContext ctx = new WebappContext("Test", CONTEXT);
            ServletRegistration servletRegistration = ctx.addServlet("intervalServlet", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                    HttpSession httpSession1 = req.getSession(true);
                    httpSession1.setMaxInactiveInterval(MAX_INACTIVE_INTERVAL);
                    assertEquals(MAX_INACTIVE_INTERVAL, httpSession1.getMaxInactiveInterval());
                    HttpSession httpSession2 = req.getSession(true);
                    assertEquals(httpSession1, httpSession2);
                }
            });
            
            servletRegistration.addMapping(SERVLETMAPPING);
            ctx.deploy(httpServer);
            
            //build and send request
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Cookie", JSESSIONID_COOKIE_NAME+"="+JSESSIONID_COOKIE_VALUE);
            HttpPacket request = ClientUtil.createRequest(CONTEXT+SERVLETMAPPING, PORT, headers);
            
            //get response update JSESSIONID if needed
            System.out.println("Sending Request with SessionId: "+headers.get("Cookie"));
            HttpContent response = ClientUtil.sendRequest(request, 60, PORT);
            updateJSessionCookies(response);
            String cookieValue1 = JSESSIONID_COOKIE_VALUE;
            System.out.println("---");
            Thread.sleep(10000);
            headers.clear();
            headers.put("Cookie", JSESSIONID_COOKIE_NAME+"="+JSESSIONID_COOKIE_VALUE);
            request = ClientUtil.createRequest(CONTEXT+SERVLETMAPPING, PORT, headers);
            response = ClientUtil.sendRequest(request, 60, PORT);
            updateJSessionCookies(response);
            String cookieValue2 = JSESSIONID_COOKIE_VALUE;
            assertEquals(cookieValue1, cookieValue2);
            
        } finally {
            stopHttpServer();
        }
    }
    
    private void updateJSessionCookies(HttpContent response) {
        HttpHeader responseHeader = response.getHttpHeader();
        MimeHeaders mimeHeaders = responseHeader.getHeaders();
        Iterable<String> values = mimeHeaders.values(Header.SetCookie);
        for (String value : values) {
            if(value.startsWith("JSESSIONID=")) {
                JSESSIONID_COOKIE_VALUE = value.substring(value.indexOf("=")+1);
                System.out.println("Updated JSESSIONID to: "+JSESSIONID_COOKIE_VALUE);
                break;
            }
        }
    }

}