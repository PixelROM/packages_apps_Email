/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.email.mail.internet;

import com.android.email.mail.BodyPart;
import com.android.email.mail.MessageTestUtils;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;
import com.android.email.mail.MessageTestUtils.MessageBuilder;
import com.android.email.mail.MessageTestUtils.MultipartBuilder;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

/**
 * This is a series of unit tests for the MimeUtility class.  These tests must be locally
 * complete - no server(s) required.
 */
@SmallTest
public class MimeUtilityTest extends TestCase {

    /** up arrow, down arrow, left arrow, right arrow */
    private final String SHORT_UNICODE = "\u2191\u2193\u2190\u2192";
    private final String SHORT_UNICODE_ENCODED = "=?UTF-8?B?4oaR4oaT4oaQ4oaS?=";
    
    /** a string without any unicode */
    private final String SHORT_PLAIN = "abcd";
    
    /** a typical no-param header */
    private final String HEADER_NO_PARAMETER = 
            "header";
    /** a typical multi-param header */
    private final String HEADER_MULTI_PARAMETER = 
            "header; Param1Name=Param1Value; Param2Name=Param2Value";

    /**
     * Test that decode/unfold is efficient when it can be
     */
    public void testEfficientUnfoldAndDecode() {
        String result1 = MimeUtility.unfold(SHORT_PLAIN);
        String result2 = MimeUtility.decode(SHORT_PLAIN);
        String result3 = MimeUtility.unfoldAndDecode(SHORT_PLAIN);
        
        assertSame(SHORT_PLAIN, result1);
        assertSame(SHORT_PLAIN, result2);
        assertSame(SHORT_PLAIN, result3);
    }

    // TODO:  more tests for unfold(String s)
        
    /**
     * Test that decode is working for simple strings
     */
    public void testDecodeSimple() {
        String result1 = MimeUtility.decode(SHORT_UNICODE_ENCODED);
        assertEquals(SHORT_UNICODE, result1);
    }
    
    // TODO:  tests for decode(String s)

    /**
     * Test that unfoldAndDecode is working for simple strings
     */
    public void testUnfoldAndDecodeSimple() {
        String result1 = MimeUtility.unfoldAndDecode(SHORT_UNICODE_ENCODED);
        assertEquals(SHORT_UNICODE, result1);
    }
    
    // TODO:  tests for unfoldAndDecode(String s)

    /**
     * Test that fold/encode is efficient when it can be
     */
    public void testEfficientFoldAndEncode() {
        String result1 = MimeUtility.foldAndEncode(SHORT_PLAIN);
        String result2 = MimeUtility.foldAndEncode2(SHORT_PLAIN, 10);
        String result3 = MimeUtility.fold(SHORT_PLAIN, 10);
        
        assertSame(SHORT_PLAIN, result1);
        assertSame(SHORT_PLAIN, result2);
        assertSame(SHORT_PLAIN, result3);
    }

    // TODO:  more tests for foldAndEncode(String s)

    /**
     * Test that foldAndEncode2 is working for simple strings
     */
    public void testFoldAndEncode2() {
        String result1 = MimeUtility.foldAndEncode2(SHORT_UNICODE, 10);
        assertEquals(SHORT_UNICODE_ENCODED, result1);
    }
    
    // TODO:  more tests for foldAndEncode2(String s)
    // TODO:  more tests for fold(String s, int usedCharacters)
    
    /**
     * Basic tests of getHeaderParameter()
     * 
     * Typical header value:  multipart/mixed; boundary="----E5UGTXUQQJV80DR8SJ88F79BRA4S8K"
     * 
     * Function spec says:
     *  if header is null:  return null
     *  if name is null:    if params, return first param.  else return full field
     *  else:               if param is found (case insensitive) return it
     *                        else return null
     */
    public void testGetHeaderParameter() {
        // if header is null, return null
        assertNull("null header check", MimeUtility.getHeaderParameter(null, "name"));
        
        // if name is null, return first param or full header
        // NOTE:  The docs are wrong - it returns the header (no params) in that case
//      assertEquals("null name first param per docs", "Param1Value", 
//              MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, null));
        assertEquals("null name first param per code", "header", 
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, null));
        assertEquals("null name full header", HEADER_NO_PARAMETER, 
                MimeUtility.getHeaderParameter(HEADER_NO_PARAMETER, null));
        
        // find name 
        assertEquals("get 1st param", "Param1Value", 
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "Param1Name"));
        assertEquals("get 2nd param", "Param2Value", 
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "Param2Name"));
        assertEquals("get missing param", null, 
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "Param3Name"));
        
        // case insensitivity
        assertEquals("get 2nd param all LC", "Param2Value", 
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "param2name"));
        assertEquals("get 2nd param all UC", "Param2Value", 
                MimeUtility.getHeaderParameter(HEADER_MULTI_PARAMETER, "PARAM2NAME"));
    }
    
    // TODO:  tests for findFirstPartByMimeType(Part part, String mimeType)

    /** Tests for findPartByContentId(Part part, String contentId) */
    public void testFindPartByContentIdTestCase() throws MessagingException, Exception {
        final String cid1 = "cid.1@android.com";
        final Part cid1bp = MessageTestUtils.bodyPart("image/gif", cid1);
        final String cid2 = "cid.2@android.com";
        final Part cid2bp = MessageTestUtils.bodyPart("image/gif", "<" + cid2 + ">");

        final Message msg1 = new MessageBuilder()
            .setBody(new MultipartBuilder("multipart/related")
                 .addBodyPart(MessageTestUtils.bodyPart("text/html", null))
                 .addBodyPart((BodyPart)cid1bp)
                 .build())
            .build();
        // found cid1 part
        final Part actual1_1 = MimeUtility.findPartByContentId(msg1, cid1);
        assertEquals("could not found expected content-id part", cid1bp, actual1_1);

        final Message msg2 = new MessageBuilder()
            .setBody(new MultipartBuilder("multipart/mixed")
                .addBodyPart(MessageTestUtils.bodyPart("image/tiff", "cid.4@android.com"))
                .addBodyPart(new MultipartBuilder("multipart/related")
                    .addBodyPart(new MultipartBuilder("multipart/alternative")
                        .addBodyPart(MessageTestUtils.bodyPart("text/plain", null))
                        .addBodyPart(MessageTestUtils.bodyPart("text/html", null))
                        .buildBodyPart())
                    .addBodyPart((BodyPart)cid1bp)
                    .buildBodyPart())
                .addBodyPart(MessageTestUtils.bodyPart("image/gif", "cid.3@android.com"))
                .addBodyPart((BodyPart)cid2bp)
                .build())
            .build();
        // found cid1 part
        final Part actual2_1 = MimeUtility.findPartByContentId(msg2, cid1);
        assertEquals("found part from related multipart", cid1bp, actual2_1);

        // found cid2 part
        final Part actual2_2 = MimeUtility.findPartByContentId(msg2, cid2);
        assertEquals("found part from mixed multipart", cid2bp, actual2_2);
    }
    
    /** Tests for getTextFromPart(Part part) */
    public void testGetTextFromPartContentTypeCase() throws MessagingException {
        final String theText = "This is the text of the part";
        TextBody tb = new TextBody(theText);
        MimeBodyPart p = new MimeBodyPart();
        
        // 1. test basic text/plain mode
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain");
        p.setBody(tb);
        String gotText = MimeUtility.getTextFromPart(p);
        assertEquals(theText, gotText);
        
        // 2. mixed case is OK
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "TEXT/PLAIN");
        p.setBody(tb);
        gotText = MimeUtility.getTextFromPart(p);
        assertEquals(theText, gotText);
        
        // 3. wildcards OK
        p.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/other");
        p.setBody(tb);
        gotText = MimeUtility.getTextFromPart(p);
        assertEquals(theText, gotText);
    }
    // TODO: Tests of charset decoding in getTextFromPart()
    
    /** Tests for various aspects of mimeTypeMatches(String mimeType, String matchAgainst) */
    public void testMimeTypeMatches() {
        // 1. No match
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", "TEXT/PLAIN"));
        
        // 2. Match
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "text/plain"));
        
        // 3. Match (mixed case)
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "TEXT/PLAIN"));
        assertTrue(MimeUtility.mimeTypeMatches("TEXT/PLAIN", "text/plain"));
        
        // 4. Match (wildcards)
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "*/plain"));
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "text/*"));
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", "*/*"));
        
        // 5. No Match (wildcards)
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", "*/plain"));
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", "text/*"));
    }
    
    /** Tests for various aspects of mimeTypeMatches(String mimeType, String[] matchAgainst) */
    public void testMimeTypeMatchesArray() {
        // 1. Zero-length array
        String[] arrayZero = new String[0];
        assertFalse(MimeUtility.mimeTypeMatches("text/plain", arrayZero));
        
        // 2. Single entry, no match
        String[] arrayOne = new String[] { "text/plain" };
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", arrayOne));
        
        // 3. Single entry, match
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", arrayOne));
        
        // 4. Multi entry, no match
        String[] arrayTwo = new String[] { "text/plain", "match/this" };
        assertFalse(MimeUtility.mimeTypeMatches("foo/bar", arrayTwo));
        
        // 5. Multi entry, match first
        assertTrue(MimeUtility.mimeTypeMatches("text/plain", arrayTwo));
        
        // 6. Multi entry, match not first
        assertTrue(MimeUtility.mimeTypeMatches("match/this", arrayTwo));
    }

    // TODO:  tests for decodeBody(InputStream in, String contentTransferEncoding)    
    // TODO:  tests for collectParts(Part part, ArrayList<Part> viewables, ArrayList<Part> attachments)

}
