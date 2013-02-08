package jon;

import java.io.File;
import java.io.FileWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class Convert {
  
  final String basePath;
  final File outPath;
  
  public Convert(String basePath, String outPath) {
    this.basePath = basePath;
    this.outPath = new File(outPath);
  }
  
  public static void recurseElement(Element element, final StringBuilder builder) {
    
    new NodeTraversor(new NodeVisitor(){
      public boolean isToc(Element element) {
        String cssClass = element.attr("class");
        return cssClass != null && cssClass.contains("sites-embed-type-toc");
      }
      boolean isInToc = false;
      int listDepth = 0;
      @Override
      public void head(Node node, int depth) {
        if (!isInToc){
          if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            String txt = textNode.text().replaceAll("\u00a0"," "); //non-break spaces
            
            builder.append(txt);
          } else if (node instanceof Element) {
              Element element = (Element) node;
              switch (element.tagName()){
                case "span":
                case "blockquote":
                  //ignored
                  break;
                case "ol":
                case "ul":
                   listDepth += 1;
                case "br":
                case "p":
                  builder.append("\n");
                  break;
                case "div":
                  if (isToc(element)){
                    isInToc = true;
                    builder.append("[[_TOC_]]\n");
                  }
                  builder.append("\n");
                  break;
                case "h1":
                  builder.append("\n# ");
                  break;
                case "h2":
                  builder.append("\n## ");
                  break;
                case "h3":
                  builder.append("\n### ");
                  break;
                case "h4":
                  builder.append("\n#### ");
                case "b":
                case "strong":
                  builder.append("**");
                  break;
                case "cite":
                case "i":
                case "u":
                  builder.append("*");
                  break;
                case "a":
                  builder.append('[');
                  break;
                case "li":
                  for (int i = 0; i < listDepth - 1; i++) {
                    builder.append(" ");
                  }
                  builder.append(element.parent().tagName().equals("ol") ? "1. " : "* ");
                  break;
                case "code":
                  builder.append("`");
                  break;
                case "strike":
                  builder.append("<").append(element.tagName()).append(">");
                  break;
                case "img":
                  String src = element.attr("src");
                  String alt = element.attr("alt");
                  alt = alt == null ? "" : alt;
                  if (src != null) {
                    if (src.contains("sites.google.com")) {
                      src = src;
                    }
                    
                    builder.append("![").append(alt).append("](").append(src).append(")\n");
                  }
                  
                  break;
                case "pre":
                  builder.append("```\n");
                  break;
                case "hr":
                  builder.append("\n***\n");
                  break;
                case "font":
                  String face = element.attr("face");
                  if (face != null && face.contains("monospace")){
                    builder.append("`");
                  }
                  break;
                default:
                  System.out.println("Unhandled element " + element.tagName());
              }
          }
        }
      }

      @Override
      public void tail(Node node, int depth) {
        
        if (node instanceof Element){
          Element element = (Element) node;
          if (isInToc) {
            if (isToc(element)) {
              isInToc = false;
            }
          } else {
            switch (element.tagName()){
              case "b":
              case "strong":
                builder.append("**");
                break;
              case "ol":
              case "ul":
                 listDepth -= 1;
                 break;
              case "cite":
              case "i":
              case "u":
                builder.append("*");
                break;
              
              case "strike":
                builder.append("</").append(element.tagName()).append(">");
                break;
              case "a":
                String href = element.attr("href");
                if (href != null){
                  if (href.startsWith("http")){
                    builder.append(']').append('(').append(href).append(')');                  
                  } else {
                    builder.append(']').append('(').append(fixPath(href.replaceAll("\\.\\./","")).replaceAll("-", " ")).append(')');
                  }
                  
                }
                break;
              case "pre":
                builder.append("\n```\n");
                break;
              case "code":
                builder.append("`");
                break;
              case "font":
                String face = element.attr("face");
                if (face != null && face.contains("monospace")){
                  builder.append("`");                
                }
                break;
              case "h1":
              case "h2":
              case "h3":
              case "h4":
              case "li":
                builder.append("\n");
              default:
                break;
            }
          }
        }
      }}).traverse(element);
  }
  
  public static String convertFile(File input) throws Exception {
    Document doc = Jsoup.parse(input, "UTF-8");
    Elements elements = doc.select("div[dir=ltr]");
    Element contentDiv = elements.first();
    if (contentDiv != null) {
      StringBuilder builder = new StringBuilder();
      recurseElement(contentDiv, builder);
      return builder.toString()
                            .replaceAll("\n`([^`]+)`", "\n        $1")
                            .replaceAll("\\*\\*\n", "**\n\n")
                            .trim();
    } else {
      return "";
    }
  }


  public static String fixPath(String path){
    return path.replaceAll("[/\\.]+", "-").replace("-index-html","").replace("-html","");
  }
  
  public void recurseDir(File dir) throws Exception {
    if (dir.isDirectory()) {
      for (File file : dir.listFiles()) {
        recurseDir(file);
      }
    } else {
      if (dir.getName().endsWith(".html")){
        String rebasedName = dir.getCanonicalPath().replace(basePath,"");
        String fileName = fixPath(rebasedName.replaceAll("[/\\.]+", "-").replace("-index-html","").replace("-html","")) + ".md";
        String[] dirsInPath = rebasedName.split("/");
        File fullOutPath = dirsInPath.length > 0 ? new File(outPath, dirsInPath[0]) : outPath;
        fullOutPath.mkdirs();
        FileWriter writer = new FileWriter(new File(fullOutPath, fileName));
        writer.write(convertFile(dir));
        writer.close();
      }
    }
  }
  
  public void convert() throws Exception {
    recurseDir(new File(basePath));
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("Usage: java -Dfile.encoding=UTF-8 Convert /path/to/sites /out/path");
    } else {
      new Convert(args[0], args[1]).convert();
    }
  }


}
