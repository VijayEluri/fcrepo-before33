package fedora.oai.sample;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * <p><b>Title:</b> RandomDCmetadataFactory.java</p>
 * <p><b>Description:</b> </p>
 *
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2002, 2003 by The
 * Rector and Visitors of the University of Virginia and Cornell University.
 * All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 *
 * @author cwilper@cs.cornell.edu
 * @version 1.0
 */
public class RandomDCMetadataFactory {

    private static String[] s_dcElements=new String[] {"title", "creator",
            "subject", "description", "publisher", "contributor", "date",
            "type", "format", "identifier", "source", "language", "relation",
            "coverage", "rights"};

    private ArrayList m_wordList=new ArrayList();

    public RandomDCMetadataFactory(File dictionaryFile)
            throws IOException {
        BufferedReader in=new BufferedReader(new FileReader(dictionaryFile));
        String nextLine="";
        while (nextLine!=null) {
            nextLine=in.readLine();
            if (nextLine!=null) {
                String[] words=nextLine.split(" ");
                for (int i=0; i<words.length; i++) {
                    String w=words[i];
                    if ( allLetters(w) ) {
                        m_wordList.add(w);
                    }
                }
            }
        }
        in.close();
    }

    public String get(int repeatMax, int wordMax) {
        return get(repeatMax, wordMax, m_wordList);
    }

    public static String get(int repeatMax, int wordMax,
            List wordList) {
        StringBuffer out=new StringBuffer();
        out.append("<oai_dc:dc\n"
                + "    xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n"
                + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
//                + "    xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n"
//                + "    http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n");
        for (int x=0; x<s_dcElements.length; x++) {
            String dcElement=s_dcElements[x];
            int num=1+getRandom(repeatMax);
            for (int i=0; i<num; i++) {
                out.append("<dc:" + dcElement + ">"
                        + getRandomWords(wordMax, wordList)
                        + "</dc:" + dcElement + ">\n");
            }
        }
        out.append("</oai_dc:dc>");
        return out.toString();
    }

    private static String getRandomWords(int wordMax, List wordList) {
        int count=1+getRandom(wordMax);
        StringBuffer out=new StringBuffer();
        for (int i=0; i<count; i++) {
            if (i>0) {
                out.append(" ");
            }
            out.append((String) wordList.get(getRandom(wordList.size())));
        }
        return out.toString();
    }

    public static int getRandom(int belowThis) {
        return (int) (Math.random() * belowThis);
    }

    private static boolean allLetters(String w) {
        if (w.length()==0) return false;
        String l=w.toLowerCase();
        for (int i=0; i<l.length(); i++) {
            char c=l.charAt(i);
            if (c<'a' || c>'z') {
                return false;
            }
        }
        return true;
    }

}
