/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.cas.client.validation;


import org.jasig.cas.client.PublicTestHttpServer;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorageImpl;
import org.jasig.cas.client.proxy.ProxyRetriever;

import java.io.UnsupportedEncodingException;

/**
 * Test cases for the {@link Cas20ServiceTicketValidator}.
 *
 * @author Scott Battaglia
 * @version $Revision: 11737 $ $Date: 2007-10-03 09:14:02 -0400 (Tue, 03 Oct 2007) $
 * @since 3.0
 */
public final class Cas20ServiceTicketValidatorTests extends
        AbstractTicketValidatorTests {

    private Cas20ServiceTicketValidator ticketValidator;

    private ProxyGrantingTicketStorage proxyGrantingTicketStorage;

    public Cas20ServiceTicketValidatorTests() {
        super();
    }

    public Cas20ServiceTicketValidatorTests(Cas20ServiceTicketValidator ticketValidator) {
        this.ticketValidator = ticketValidator;
    }

    protected void setUp() throws Exception {
        this.proxyGrantingTicketStorage = getProxyGrantingTicketStorage();
        this.ticketValidator = new Cas20ServiceTicketValidator(CONST_CAS_SERVER_URL);
        this.ticketValidator.setProxyCallbackUrl("test");
        this.ticketValidator.setProxyGrantingTicketStorage(getProxyGrantingTicketStorage());
        this.ticketValidator.setProxyRetriever(getProxyRetriever());
        this.ticketValidator.setRenew(true);
    }

    private ProxyGrantingTicketStorage getProxyGrantingTicketStorage() {
        final ProxyGrantingTicketStorageImpl proxyGrantingTicketStorageImpl = new ProxyGrantingTicketStorageImpl();

        return proxyGrantingTicketStorageImpl;
    }

    private ProxyRetriever getProxyRetriever() {
        final ProxyRetriever proxyRetriever = new ProxyRetriever() {

            /** Unique Id for serialization. */
			private static final long serialVersionUID = 1L;

			public String getProxyTicketIdFor(String proxyGrantingTicketId, String targetService) {
                return "test";
            }
        };

        return proxyRetriever;
    }

    public void testNoResponse() throws UnsupportedEncodingException {
        final String RESPONSE = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationFailure code=\"INVALID_TICKET\">Ticket ST-1856339-aA5Yuvrxzpv8Tau1cYQ7 not recognized</cas:authenticationFailure></cas:serviceResponse>";
        PublicTestHttpServer.instance().content = RESPONSE
                .getBytes(PublicTestHttpServer.instance().encoding);
        try {
            this.ticketValidator.validate("test", "test");
            fail("ValidationException expected due to 'no' response");
        } catch (final TicketValidationException e) {
            // expected
        }
    }

    public void testYesResponseButNoPgt() throws TicketValidationException,
            UnsupportedEncodingException {
        final String USERNAME = "username";
        final String RESPONSE = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationSuccess><cas:user>"
                + USERNAME
                + "</cas:user></cas:authenticationSuccess></cas:serviceResponse>";
        PublicTestHttpServer.instance().content = RESPONSE
                .getBytes(PublicTestHttpServer.instance().encoding);

        final Assertion assertion = this.ticketValidator.validate("test",
                "test");
        assertEquals(USERNAME, assertion.getPrincipal().getName());
    }

    public void testYesResponseWithPgt() throws TicketValidationException,
            UnsupportedEncodingException {
        final String USERNAME = "username";
        final String PGTIOU = "testPgtIou";
        final String PGT = "test";
        final String RESPONSE = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationSuccess><cas:user>"
                + USERNAME
                + "</cas:user><cas:proxyGrantingTicket>"
                + PGTIOU
                + "</cas:proxyGrantingTicket></cas:authenticationSuccess></cas:serviceResponse>";

        PublicTestHttpServer.instance().content = RESPONSE
                .getBytes(PublicTestHttpServer.instance().encoding);

        this.proxyGrantingTicketStorage.save(PGTIOU, PGT);

        final Assertion assertion = this.ticketValidator.validate("test",
                "test");
        assertEquals(USERNAME, assertion.getPrincipal().getName());
//        assertEquals(PGT, assertion.getProxyGrantingTicketId());
    }
    
    public void testGetAttributes() throws TicketValidationException,
    UnsupportedEncodingException {
        final String USERNAME = "username";
        final String PGTIOU = "testPgtIou";
        final String RESPONSE = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'><cas:authenticationSuccess><cas:user>"
            + USERNAME
            + "</cas:user><cas:proxyGrantingTicket>"
            + PGTIOU
            + "</cas:proxyGrantingTicket><cas:attributes>\n<cas:password>test</cas:password>\n<cas:eduPersonId>id</cas:eduPersonId>\n</cas:attributes></cas:authenticationSuccess></cas:serviceResponse>";
        
        PublicTestHttpServer.instance().content = RESPONSE
        .getBytes(PublicTestHttpServer.instance().encoding);

        final Assertion assertion = this.ticketValidator.validate("test", "test");
        assertEquals(USERNAME, assertion.getPrincipal().getName());
        assertEquals("test", assertion.getPrincipal().getAttributes().get("password"));
        assertEquals("id", assertion.getPrincipal().getAttributes().get("eduPersonId"));
        //assertEquals(PGT, assertion.getProxyGrantingTicketId());
    }

    public void testInvalidResponse() throws Exception {
        final String RESPONSE = "<root />";
        PublicTestHttpServer.instance().content = RESPONSE
                .getBytes(PublicTestHttpServer.instance().encoding);
        try {
            this.ticketValidator.validate("test", "test");
            fail("ValidationException expected due to invalid response.");
        } catch (final TicketValidationException e) {
            // expected
        }
    }
}
