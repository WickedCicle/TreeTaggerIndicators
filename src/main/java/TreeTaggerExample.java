/*******************************************************************************
 * Copyright (c) 2009-2014 Richard Eckart de Castilho.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Richard Eckart de Castilho - initial API and implementation
 ******************************************************************************/
//package org.annolab.tt4j;

import org.annolab.tt4j.TokenHandler;
import org.annolab.tt4j.TreeTaggerWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TreeTaggerExample {

    public static void main(String[] args) throws Exception {

        final String[] Initial = {""};
        final String[] PoS = {""};

        // Point TT4J to the TreeTagger installation directory. The executable is expected
        // in the "bin" subdirectory - in this example at "/opt/treetagger/bin/tree-tagger"
        System.setProperty("treetagger.home", "D:\\Java\\TreeTaggerIndicators\\tree-tagger-windows-3.2.3\\TreeTagger");
        TreeTaggerWrapper<String> tt = new TreeTaggerWrapper<String>();
        tt.setModel("D:\\Java\\TreeTaggerIndicators\\tree-tagger-windows-3.2.3\\TreeTagger\\models\\russian.par");
        tt.setHandler(new TokenHandler<String>() {
            public void token(String token, String pos, String lemma) {
                //System.out.println(token + "\t" + pos + "\t" + lemma);
                Initial[0] = lemma;
                PoS[0] = pos;
            }
        });

        ArrayList<String> texts = new ArrayList<>();

        Files.walk(Paths.get("D:\\Downloads\\RNC_million\\RNC_million\\sample_ar\\TEXTS"))
                .filter(Files::isRegularFile)
                .forEach((file -> {
                    texts.add(file.toString());
                }));

        AtomicInteger intUnfamilliar = new AtomicInteger();
        AtomicInteger intKnown = new AtomicInteger();
        AtomicInteger wordCount = new AtomicInteger();
        AtomicInteger accuracy = new AtomicInteger();
        AtomicBoolean isAdded = new AtomicBoolean(false);

        Instant start;
        Instant finish;
        long elapsed = 0;

        try {
            for (String text : texts) {
                System.out.println("next file" + text.toString());
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(text);

                Node html = document.getDocumentElement();

                NodeList htmlProps = html.getChildNodes();
                for (int i = 0; i < htmlProps.getLength(); i++) {
                    Node body = htmlProps.item(i);
                    if (body.getNodeType() != Node.TEXT_NODE && body.getNodeName().equals("body")) {
                        NodeList bodyProps = body.getChildNodes();
                        for (int j = 0; j < bodyProps.getLength(); j++) {
                            Node paragraph = bodyProps.item(j);
                            if (paragraph.getNodeType() != Node.TEXT_NODE && (paragraph.getNodeName().equals("p") || paragraph.getNodeName().equals("speach"))) {
                                NodeList paragraphProps = paragraph.getChildNodes();
                                for (int k = 0; k < paragraphProps.getLength(); k++) {
                                    Node sentence = paragraphProps.item(k);
                                    if (sentence.getNodeType() != Node.TEXT_NODE && sentence.getNodeName().equals("se")) {
                                        NodeList sentenceProps = sentence.getChildNodes();
                                        for (int m = 0; m < sentenceProps.getLength(); m++) {
                                            Node word = sentenceProps.item(m);
                                            if (word.getNodeType() != Node.TEXT_NODE && word.getNodeName().equals("w")) {
                                                wordCount.getAndIncrement();
                                                NodeList wordProps = word.getChildNodes();
                                                start = Instant.now();;
                                                for (int n = 0; n < wordProps.getLength(); n++) {
                                                    Node characteristics = wordProps.item(n);
                                                    if (isAdded.get()) {
                                                        continue;
                                                    }
                                                    if (characteristics.getNodeType() != Node.TEXT_NODE && characteristics.getNodeName().equals("ana")) {
                                                        tt.process(new String[] {word.getTextContent().toLowerCase(Locale.ROOT).replaceAll("[` ]", "").replaceAll("ё", "е")});
                                                        if (Objects.equals(PoS[0], "-")) {
                                                            intUnfamilliar.getAndIncrement();
                                                            isAdded.set(true);
                                                        } else {
                                                            intKnown.getAndIncrement();
                                                            if (Objects.equals(Initial[0].toLowerCase(Locale.ROOT).replaceAll("ё", "е"), characteristics.getAttributes().getNamedItem("lex").getNodeValue().toLowerCase(Locale.ROOT).replaceAll("ё", "е"))) {
                                                                accuracy.getAndIncrement();
                                                                isAdded.set(true);
                                                            }
                                                        }
                                                    }
                                                }
                                                finish = Instant.now();
                                                elapsed += Duration.between(start, finish).toMillis();
                                                isAdded.set(false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("Количество ненайдённых: " + intUnfamilliar);
            System.out.println("Количество найдённых в словаре: " + intKnown);
            System.out.println("Общее количество слов: " + wordCount);
            System.out.println("Точно определенных начальных форм слов: " + accuracy);
            System.out.println("Процент ненайдённых:" + intUnfamilliar.doubleValue() / wordCount.doubleValue());
            System.out.println("Точность: " + accuracy.doubleValue() / intKnown.doubleValue());
            System.out.println("Затраченное время: " + (double)elapsed/1000 + " секунд");
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ex.printStackTrace(System.out);
        }
    }
}
