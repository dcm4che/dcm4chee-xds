/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4chee.xds2.infoset.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This handler extracts all the documents from an XdS.b request and adds them as attachments. This
 * is a workaround because there is a bug in the way MTOM and handlers work if used together. See <a
 * href="https://java.net/jira/browse/WSIT-1320">this link</a> for more details concerning this.
 */
public class XDSDocumentAttachmentHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String XOP_NAMESPACE                      = "http://www.w3.org/2004/08/xop/include";
    private static final String MIME_TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String DOCUMENT_TAG                       = "Document";
    private static final Logger log                                = LoggerFactory.getLogger(SentSOAPLogHandler.class);

    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {

        boolean isOutboundMessage = (Boolean) ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        log.debug("##########handleMessage AttachmentSOAPHandler:" + isOutboundMessage);

        if (isOutboundMessage) {

            SOAPMessage message = ctx.getMessage();
            SOAPEnvelope envelope;

            try {
                envelope = message.getSOAPPart().getEnvelope();
                SOAPBody body = envelope.getBody();
                
                NodeList nodeList = body.getElementsByTagNameNS(BasePortTypeFactory.URN_IHE_ITI, DOCUMENT_TAG);

                for (int i = 0; i < nodeList.getLength(); i++) {

                    Element element = (Element) nodeList.item(i);
                    if(element == null) {
                        continue;
                    }
                    // The first child may not be null as there is at least one attribute at the document
                    String elementContent = element.getFirstChild().getNodeValue();
                    InputStream is = new ByteArrayInputStream(elementContent.getBytes());

                    UUID ref = UUID.randomUUID();

                    AttachmentPart attachment = message.createAttachmentPart();
                    attachment.setBase64Content(is, MIME_TYPE_APPLICATION_OCTET_STREAM);
                    attachment.setContentId(ref.toString());
                    message.addAttachmentPart(attachment);

                    SOAPBodyElement bodyElement = (SOAPBodyElement) element;
                    bodyElement.removeContents();
                    bodyElement.addChildElement("Include", "xop", XOP_NAMESPACE).setAttribute("href",
                                                                                              "cid:" + ref.toString());
                }
            } catch (SOAPException e) {
                log.debug("error extracting attachments in AttachmentSOAPHandler", e);
                return false;
            } finally {
                message = null;
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext ctx) {
        boolean isOutboundMessage = (Boolean) ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        log.debug("##########handleMessage AttachmentSOAPHandler:" + isOutboundMessage);
        return true;
    }

    @Override
    public void close(MessageContext context) {}

    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}
