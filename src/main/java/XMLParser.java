import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMLParser {
    public static void main(String[] args) {
       /* try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.normalizeDocument();
            for (int i=0; i<doc.getElementsByTagName("*").getLength(); i++) {
                // Get element
                Element element = (Element)doc.getElementsByTagName("*").item(i);
                Stack<String> elements = new Stack<>();
                elements.push(element.getTagName());
                System.out.println(elements);
            }
        }catch (Exception e){
            e.printStackTrace();
        }*/
        File fXmlFile = new File("prueba.xml");
        File dtdFile = new File("prueba.dtd");


        System.out.println("COMPLETE TAGS: " + getAllTags(fXmlFile));
        System.out.println("TAGS: " + getTags(fXmlFile));
        for (String tag: getAllTags(fXmlFile)){
            System.out.println("TAG: " + tag + " : ATT: " +   getAttributes(tag));
            System.out.println("TAG: " + tag + " : TRASH: " +   getTrash(tag));
            System.out.println("DELETE ATT AND TAGS" +   deleteAttributesAndTrashFromTag(tag));
            System.out.println("TAGS WITHOUT SINGLE TAGS" + getTagsWithoutSingleTags(fXmlFile));
        }

        validateXML(fXmlFile);
        System.out.println(getDTDTags(dtdFile));
        System.out.println(getElementTags(dtdFile));
        System.out.println(getChildsFromTag("<!ELEMENT DAY (DATE,(HOLIDAY|PROGRAMSLOT+)+)>"));
    }

    /*public static void validateXML(File f) {
        try{
            String xml = new String(Files.readAllBytes(f.toPath()));
            boolean valido = true;
            for (String element : getElements(f)) {
                Matcher m = Pattern.compile("<"+element+">" +"(.*?)"+"</"+element+">").matcher(xml);
                if(!m.find()){
                    System.out.println("ERROR: El elemento " + element + " no es válido");
                    valido = false;
                }
                if(valido){
                    System.out.println("El archivo XML esta formado correctamente");
                }
            }
        }catch (IOException e){
        }
    }
    */
   public static Stack<String> validateXMLStack(File f){
        Stack<String> stack = new Stack();
        for (String tag: getTags(f)){
            if(tag.charAt(1) != '/'){
                stack.push(tag);
            }else{
                if(stack.empty()){
                    System.out.println("ERROR, falta la etiqueta de apetura de " + getTagName(tag));
                    return stack;
                }else if(getTagName(tag).equals(getTagName(stack.peek()))){
                    stack.pop();
                }else{
                    System.out.println("ERROR, falta la etiqueta de cierre " + getTagName(stack.peek()));
                    return stack;
                }
            }
        }
        if(stack.size() > 0){
            System.out.println("ERROR, falta la etiqueta de cierre de " + getTagName(stack.pop()));
            for (String tag:stack) {
                System.out.println("Tags en la pila: " + tag);
            }
        }
        return stack;
    }

   public static Stack<String> validateXML(File f){
       Stack<String> stack = new Stack();
       if(validateTags(getAllTags(f)) && validateTrash(f) && validateDuplicateAttributes(f)){
           for (String tag: getTagsWithoutSingleTags(f)){
               System.out.println(tag);
               if(tag.charAt(1) != '/'){
                   stack.push(tag);
               }else{
                   if(stack.empty()){
                       System.out.println("El elemento en el documento que sigue al elemento raíz debe estar bien formado");
                       return stack;
                   }
                   else if(getTagName(tag).equals(getTagName(stack.peek()))){
                       stack.pop();
                   }else{
                       System.out.println("El tipo de elemento "+getTagName(stack.peek())+" debe terminar con la etiqueta final correspondiente </"+getTagName(stack.peek())+">");
                       return stack;
                   }
               }
           }
           if(stack.size() > 0){
               System.out.println("Los documentos XML deben comenzar y terminar dentro de la misma entidad(" + getTagName(stack.peek())+")");
               return stack;
           }
           System.out.println("El archivo XML está bien formado.");
           return stack;
       }else{
           return stack;
       }
   }



   /* public static void validateXML2(File f) {
        try{
            String xml = new String(Files.readAllBytes(f.toPath()));
            System.out.println(xml);
            for (String element : getElements(f)) {
                Pattern p = Pattern.compile("<"+element+">" +"(.+?)"+"</"+element+">");
                Matcher m = p.matcher(xml);
                if(!m.find()){
                    System.out.println("Error, el elemento " + m.group() + "no existe");
                }
            }
        }catch (IOException e){
        }
    }
    */

    public static List<String> getTags(File f){
        List<String> tags = new LinkedList<>();
        String xml = normalizarXML(f);
        Matcher m = Pattern.compile("(<[^?](\\S+?)(.*?)[/]?>)").matcher(xml);
        String linea = null;
        String sinTrashAndAtt = null;
        while(m.find()){
            if(getAttributes(m.group()).isEmpty()){
                tags.add(deleteTrash(m.group()));
            }else{
                linea = m.group();
                tags.add(deleteAttributesAndTrashFromTag(m.group()));
                sinTrashAndAtt = deleteAttributesAndTrashFromTag(m.group());

            }
        }
        return tags;
    }

    public static List<String> getStartTags(List<String> tags){
        List<String> startTags = new LinkedList<>();
        for (String openTag: tags) {
            if(openTag.charAt(1) != '/')
                startTags.add(openTag);
        }
        return startTags;
    }

    public static List<String> getCloseTags(List<String> tags){
        List<String> closeTags = new LinkedList<>();
        for (String closeTag: tags) {
            if(closeTag.charAt(1) == '/')
                closeTags.add(closeTag);
        }
        return closeTags;
    }

    public static String normalizarXML(File f){
        String xml = null;
        try {
            xml = new String(Files.readAllBytes(f.toPath())).replaceAll("(?m)^\\s+|\n|\r", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xml;
    }

    public static String getTagName(String tag){
        Matcher m = Pattern.compile("<[/]*(\\S+?)>").matcher(tag);
        String tagName = null;
        if(m.find()){
            tagName = m.group(1);
        }
        return tagName;
    }
    public static List<String> getAttributes(String tag){
        Matcher m = Pattern.compile("(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?").matcher(tag);
        List<String> attributes = new LinkedList<>();
        while(m.find()){
            attributes.add(m.group());
        }
        return attributes;
    }

    public static List<String> getAttributesNames(String tag){
        Matcher m = Pattern.compile("(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?").matcher(tag);
        List<String> attributes = new LinkedList<>();
        while(m.find()){
            attributes.add(m.group(1));
        }
        return attributes;
    }




    public static String deleteAttributes(String tag){
        String auxtag = tag;
        if(!getAttributes(tag).isEmpty()){
            for (String att: getAttributes(tag)){
                auxtag = auxtag.replaceAll(att,"");
            }
            if(getTrash(tag).isEmpty()){
                auxtag = auxtag.replaceAll("\\s","");
            }
        }
        return auxtag;
    }

    public static List<String> getAllTags(File f){
        List<String> tags = new LinkedList<>();
        String xml = normalizarXML(f);
        Matcher m = Pattern.compile("<[^?]([^>]+)[/]?>").matcher(xml);
        while(m.find()){
            tags.add(m.group());
        }
        return tags;
    }

    public static List<String> getTagsWithoutSingleTags(File f){
        List<String> tags = new LinkedList<>();
        for (String tag: getTags(f)){
            if(!tag.contains("/>")){
                tags.add(tag);
            }
        }
        return tags;
    }

    public static String deleteTrash(String tag){
        Matcher m = Pattern.compile("\\s([^>]*)[/>]?").matcher(tag);
        String auxtag = tag;
        while(m.find()){
            auxtag = auxtag.replaceAll(m.group(1).replace("/",""),"");
        }
        return auxtag.replaceAll("\\s","");
    }
    public static List<String> getTrash(String tag){
        List trash = new LinkedList();
        Matcher m = Pattern.compile("\\s([^>]*)([^/>])?").matcher(tag);
        while(m.find()){
            for (String tr: deleteAttributes(m.group(1)).split("\\s")){
                if(!tr.equals(""))
                    trash.add(tr);
            }
        }
        return trash;
    }

    public static boolean validateTag(String tag){
        //Matcher matcher = Pattern.compile("<[^?](\"[^\"]*\"|'[^']*'|[^'\">])*>").matcher(tag);
        Matcher matcher = Pattern.compile("<[^?](\"[^\"]*\"|'[^']*'|[^'\">])*>").matcher(tag);
        return matcher.matches();
    }
    public static boolean validateTags(List<String> tags){
        boolean valido = true;
        for (String tag: tags){
            if(!validateTag(tag)){
                System.out.println("ERROR, la etiqueta " + tag +" no esta bien formada");
                valido = false;
            }
        }
        return valido;
    }

    public static String deleteAttributesAndTrashFromTag(String tag){
        return deleteTrash(deleteAttributes(tag));
    }
    public static boolean validateTrash(File f){
        boolean valido = true;
        for (String tag: getAllTags(f)){
            if(getTrash(deleteAttributes(tag)).size() > 0){
                System.out.println("El nombre del atributo "+getTrash(tag).get(0)+" asociado con un tipo de elemento "+ tag + " debe ir seguido por el carácter '='.");
                valido = false;
            }
        }
        return valido;
    }

    public static boolean findDuplicateAttributes(String tag) {
        if(getDuplicateAttributes(tag).size() > 0)
            return true;
        return false;
    }

    public static List<String> getDuplicateAttributes(String tag){
        List<String> duplicates = new LinkedList<>();
        Set<String> attributes = new LinkedHashSet<>();
        for (String att: getAttributesNames(tag)){
            if(!attributes.add(att)){
                duplicates.add(att);
            }
        }
        return duplicates;
    }
    public static boolean validateDuplicateAttributes(File f){
        boolean valido = true;
        for (String tag: getAllTags(f)){
            if(findDuplicateAttributes(tag)){
                System.out.println("El atributo "+ getDuplicateAttributes(tag).get(0)+" ya estaba especificado para el elemento " + getTagName(deleteAttributesAndTrashFromTag(tag)));
                valido= false;
            }
        }
        return valido;
    }
    public static List<String> getDTDTags(File f){
        List<String> tags = new LinkedList<>();
        String xml = normalizarXML(f);
        Matcher m = Pattern.compile("<!([^>\\[]+)>").matcher(xml);
        while(m.find()){
            tags.add(m.group());
        }
        return tags;
    }

    public static List<String> getElementTags(File f){
        List<String> elementTags = new LinkedList<>();
        for (String dtdTag: getDTDTags(f)) {
            if (dtdTag.contains("<!ELEMENT")) {
                elementTags.add(dtdTag);
            }
        }
        return elementTags;
    }

    public static String getDTDTagName(String tag){
        Matcher m = Pattern.compile("<![\\S+?]+[\\s]+([\\S+?]+)[\\s]+[^>]+>").matcher(tag);
        String dtdTagName = null;
        if(m.find()){
            dtdTagName = m.group(1);
        }
        return dtdTagName;
    }

    public static List<String> getChildsFromTag(String tag){
        List<String> childs = new LinkedList<>();
        Matcher m = Pattern.compile("<![\\S+?]+[\\s]+([\\S+?]+)[\\s]+[\\(]?([^>]+)>").matcher(tag);
        //Opcion 2 [\(]*[A-ZAa-z\(\|\+\?\*]+
        while(m.find()){
            if(m.group(2).contains(" ")){
                for (String att: m.group(2).split(" ")){
                    childs.add(att);
                }
            }else{
                childs.add(m.group(2));
            }
        }
        return childs;
    }

}
