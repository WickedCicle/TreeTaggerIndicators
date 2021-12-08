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

        HashMap<String, String> tags = new HashMap<>();

        fillTags(tags);

        final String[] Initial = {""};
        final String[] PoS = {""};

        // Point TT4J to the TreeTagger installation directory. The executable is expected
        // in the "bin" subdirectory - in this example at "/opt/treetagger/bin/tree-tagger"
        System.setProperty("treetagger.home", "D:\\Java\\TreeTaggerIndicators\\tree-tagger-windows-3.2.3\\TreeTagger");
        TreeTaggerWrapper<String> tt = new TreeTaggerWrapper<>();
        tt.setModel("D:\\Java\\TreeTaggerIndicators\\tree-tagger-windows-3.2.3\\TreeTagger\\models\\russian.par");
        tt.setHandler((token, pos, lemma) -> {
            Initial[0] = lemma;
            PoS[0] = pos;
        });

        ArrayList<String> texts = new ArrayList<>();

        Files.walk(Paths.get("D:\\Downloads\\RNC_million\\RNC_million\\sample_ar\\TEXTS"))
                .filter(Files::isRegularFile)
                .forEach((file -> {
                    texts.add(file.toString());
                }));

        AtomicInteger intUnfamilliar = new AtomicInteger(); // ненайденные в словаре
        AtomicInteger intKnown = new AtomicInteger(); // найденные в словаре
        AtomicInteger wordCount = new AtomicInteger(); // суммарное количесвто слов
        AtomicInteger accuracy = new AtomicInteger(); // точно определённые слова
        AtomicInteger morphAccuracy = new AtomicInteger(); // первая же форма с подходящими морфологическими хар-ками
        AtomicBoolean isAdded = new AtomicBoolean(false);

        Instant start;
        Instant finish;
        long elapsed = 0;

        try {
            for (String text : texts) {
                System.out.println("next file" + text);
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
                                                start = Instant.now();
                                                for (int n = 0; n < wordProps.getLength(); n++) {
                                                    Node characteristics = wordProps.item(n);
                                                    if (isAdded.get()) {
                                                        continue;
                                                    }
                                                    if (characteristics.getNodeType() != Node.TEXT_NODE && characteristics.getNodeName().equals("ana")) {
                                                        tt.process(new String[] {word.getTextContent().toLowerCase(Locale.ROOT).replaceAll("[` ]", "").replaceAll("ё", "е")});
                                                        if (Objects.equals(PoS[0], "-")) {
                                                            intUnfamilliar.getAndIncrement();
                                                        } else {
                                                            intKnown.getAndIncrement();
                                                            if (Objects.equals(Initial[0].toLowerCase(Locale.ROOT).replaceAll("ё", "е"), characteristics.getAttributes().getNamedItem("lex").getNodeValue().toLowerCase(Locale.ROOT).replaceAll("ё", "е"))) {
                                                                accuracy.getAndIncrement();
                                                            }

                                                            isAdded.set(true);

                                                            String transformTag;

                                                            transformTag = tagDefinition(PoS[0]);

                                                            String[] transformTagSplit = transformTag.split("[,]");
                                                            StringBuilder temp = new StringBuilder();

                                                            for (String value : transformTagSplit) {
                                                                temp.append(tags.get(value));
                                                                temp.append(",");
                                                            }
                                                            temp = new StringBuilder(temp.substring(0, temp.length() - 1));

                                                            String[] transformedTag = temp.toString().split(",");

                                                            List<String> list = new ArrayList<>();
                                                            for (String s : transformedTag) {
                                                                if (s != null && !Objects.equals(s, "null") && !s.equals("0") && s.length() > 0) {
                                                                    list.add(s);
                                                                }
                                                            }
                                                            transformedTag = list.toArray(new String[0]);

                                                            String[] markTags = characteristics.getAttributes().getNamedItem("gr").getNodeValue()
                                                                    .replaceAll("distort", "").replaceAll("persn", "")
                                                                    .replaceAll("patrn", "").replaceAll("indic", "")
                                                                    .replaceAll("imper", "").replaceAll("abbr", "")
                                                                    .replaceAll("ciph", "").replaceAll("INIT", "")
                                                                    .replaceAll("anom", "").replaceAll("famn", "")
                                                                    .replaceAll("zoon", "").replaceAll("pass", "")
                                                                    .replaceAll("inan", "").replaceAll("anim", "")
                                                                    .replaceAll("intr", "").replaceAll("tran", "")
                                                                    .replaceAll("act", "").replaceAll("ipf", "")
                                                                    .replaceAll("med", "").replaceAll("pf", "")
                                                                    .split("[,=]");

                                                            list = new ArrayList<>();
                                                            for (String s : markTags) {
                                                                if (s != null && !Objects.equals(s, "null") && !s.equals("0") && s.length() > 0) {
                                                                    list.add(s);
                                                                }
                                                            }
                                                            markTags = list.toArray(new String[0]);

                                                            for (String markTag : markTags) {
                                                                if (!Arrays.asList(transformedTag).contains(markTag)) {
                                                                    isAdded.set(false);
                                                                }
                                                            }

                                                            if (isAdded.get()){
                                                                morphAccuracy.getAndIncrement();
                                                            }
                                                        }
                                                        isAdded.set(true);
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
            System.out.println("Точно определенных форм слов с полными характеристиками: " + morphAccuracy);
            System.out.println("Процент ненайдённых:" + intUnfamilliar.doubleValue()/wordCount.doubleValue());
            System.out.println("Точность начальных форм: " + accuracy.doubleValue()/intKnown.doubleValue());
            System.out.println("Точность определения характеристик первой формы: " + morphAccuracy.doubleValue()/intKnown.doubleValue());
            System.out.println("Затраченное время: " + (double)elapsed/1000 + " секунд");

        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ex.printStackTrace(System.out);
        }
    }

    static void fillTags(HashMap<String, String> tags) {
        tags.put("PoS=Noun", "S");
        tags.put("PoS=Verb", "V");
        tags.put("PoS=Adjective", "A");
        tags.put("PoS=Pronoun","PRAEDICPRO");
        tags.put("PoS=Adverb","ADV");
        tags.put("PoS=Adposition","PR");
        tags.put("PoS=Conjunction","CONJ");
        tags.put("PoS=CATEGORY","NUM,ANUM");
        tags.put("PoS=Particle","PART");
        tags.put("PoS=Interjection","INTJ");
        tags.put("Gender=m", "m");
        tags.put("Gender=f", "f");
        tags.put("Gender=n", "n");
        tags.put("Gender=c", "m-f");
        tags.put("Number=s", "sg");
        tags.put("Number=p", "pl");
        tags.put("Case=n", "nom");
        tags.put("Case=g", "gen");
        tags.put("Case=d", "dat");
        tags.put("Case=a", "acc");
        tags.put("Case=v", "voc");
        tags.put("Case=l", "loc");
        tags.put("Case=i", "ins");
        tags.put("Animate=n", "inan");
        tags.put("Animate=y", "anim");
        tags.put("VForm=i", "indic");
        tags.put("VForm=m", "imper,imper2");
        tags.put("VForm=n", "inf");
        tags.put("VForm=p", "partcp");
        tags.put("VForm=g", "ger");
        tags.put("Tense=p", "praes");
        tags.put("Tense=f", "fut");
        tags.put("Tense=s", "praet");
        tags.put("Person=1", "1p");
        tags.put("Person=2", "2p");
        tags.put("Person=3", "3p");
        tags.put("Voice=a", "act");
        tags.put("Voice=p", "pass");
        tags.put("Voice=m", "med");
        tags.put("Definiteness=s", "brev");
        tags.put("Definiteness=f", "plen");
        tags.put("Degree=c", "comp,comp2");
        tags.put("Degree=s", "supr");
        tags.put("Syntactic_Type=n", "SPRO");
        tags.put("Syntactic_Type=a", "APRO");
        tags.put("Syntactic_Type=r", "ADVPRO");
    }

    static String tagDefinition(String tags){
        String decryptTags = "";
        if (tags.charAt(0) == 'N') {
            decryptTags += "PoS=Noun";
            decryptTags += ",Gender=" + tags.charAt(2);
            decryptTags += ",Number=" + tags.charAt(3);
            decryptTags += ",Case=" + tags.charAt(4);
            decryptTags += ",Animate=" + tags.charAt(5);
        } else if (tags.charAt(0) == 'V') {
            decryptTags += "PoS=Verb";
            decryptTags += ",VForm=" + tags.charAt(2);
            decryptTags += ",Tense=" + tags.charAt(3);
            decryptTags += ",Person=" + tags.charAt(4);
            decryptTags += ",Number=" + tags.charAt(5);
            decryptTags += ",Gender=" + tags.charAt(6);
            decryptTags += ",Voice=" + tags.charAt(7);
            decryptTags += ",Definiteness=" + tags.charAt(8);
            decryptTags += ",Aspect=" + tags.charAt(9);
            if (tags.length() == 11){
                    decryptTags += ",Case=" + tags.charAt(10);
            }
        } else if (tags.charAt(0) == 'A') {
            decryptTags += "PoS=Adjective";
            decryptTags += ",Degree=" + tags.charAt(2);
            decryptTags += ",Gender=" + tags.charAt(3);
            decryptTags += ",Number=" + tags.charAt(4);
            decryptTags += ",Case=" + tags.charAt(5);
            decryptTags += ",Definiteness=" + tags.charAt(6);
        } else if (tags.charAt(0) == 'P'){
            decryptTags += "PoS=Pronoun";
            decryptTags += ",Person=" + tags.charAt(2);
            decryptTags += ",Gender=" + tags.charAt(3);
            decryptTags += ",Number=" + tags.charAt(4);
            decryptTags += ",Case=" + tags.charAt(5);
            decryptTags += ",Syntactic_Type=" + tags.charAt(6);
            if (tags.length() > 7) {
                decryptTags += ",Animate=" + tags.charAt(7);
            }
        } else if (tags.charAt(0) == 'R') {
            decryptTags += "PoS=Adverb";
            if (tags.length() > 1) {
                decryptTags += ",Degree=" + tags.charAt(1);
            }
        } else if (tags.charAt(0) == 'S') {
            decryptTags += "PoS=Adposition";
            decryptTags += ",Formation=" + tags.charAt(2);
            decryptTags += ",Case=" + tags.charAt(3);
        } else if (tags.charAt(0) == 'C') {
            decryptTags += "PoS=Conjunction";
        } else if (tags.charAt(0) == 'M') {
            decryptTags += "PoS=CATEGORY";
            if (tags.length() > 2) {
                decryptTags += ",Gender=" + tags.charAt(2);
                decryptTags += ",Number=" + tags.charAt(3);
                decryptTags += ",Case=" + tags.charAt(4);
            }
        }else if (tags.charAt(0) == 'Q') {
            decryptTags += "PoS=Particle";
        }else if (tags.charAt(0) == 'I') {
            decryptTags += "PoS=Interjection";
        }
        return decryptTags;
    }
}