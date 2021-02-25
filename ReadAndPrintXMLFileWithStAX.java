package apps.frontdoor.components.content;

import org.apache.commons.lang.StringUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadAndPrintXMLFileWithStAX {

    public static void main(String argv[]) throws Exception {
        String pathToRoot = "ui.apps/src/main/content/jcr_root/apps/frontdoor/components/content";
        getAllEpamComponents(new File(pathToRoot), 0);
//        System.out.println(allEpamComponents.toString());
//        getFiles(".html", new File(pathToRoot));
    }

    //This is working code for files with some extension
    public static List<File> getFiles(String extension, final File folder) {
        extension = extension.toUpperCase();
        final List<File> files = new ArrayList<>();
        if (folder == null || folder.listFiles() == null) {
            return files;
        }
        for (final File file : folder.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(getFiles(extension, file));
            } else if (file.getName().toUpperCase().endsWith(extension)) {
                files.add(file);
//                System.out.println(file.getName());
            }

        }
        return files;
    }

    public static List<File> getAllComponents(File parent, int depth) {
        List<File> dirs = new ArrayList<>();
        File[] files = parent.listFiles();
        if (files == null) {
            return dirs;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                if (depth == 0) {
                    dirs.add(f);
                }
            }
        }
        return dirs;
    }

    public static Set<File> getAllEpamComponents(File parent, int depth) throws XMLStreamException {
        Set<File> epams = new HashSet<>();
        List<File> allComponents = getAllComponents(parent, depth);
//        Map<String, File> epamNamedComponents = null;
        for (File f : allComponents) {
            if (Files.isDirectory(f.toPath()) && !f.getName().equals("_cq_dialog")) {
                File contentXml = Arrays.stream(Objects.requireNonNull(f.listFiles())).findFirst().get();
                getAllEpamComponents(f, 0);
                getEpamNamedComponents(contentXml);
            }
        }
//        System.out.println(epamNamedComponents);
        return epams;
    }

    private static Map<String, File> getEpamNamedComponents(File contentXml) {
        BufferedReader reader = null;
        Map<String, File> epamComponents = new HashMap<>();
        List<String> allEpamComponentsNamesList = null;
        List<File> componentsPaths = new ArrayList<>();
        String componentName;
        try {
            reader = new BufferedReader(new FileReader(contentXml));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().contains("EPAM")) {
                    line = line.trim();
                    componentName = line.substring(line.indexOf("\"")).replace("\"", "");
                    allEpamComponentsNamesList = getAllEpamComponentsNamesList(componentName)
                            .stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    componentsPaths.add(contentXml.getParentFile());
                    for (String s : allEpamComponentsNamesList) {
                        for (File componentsPath : componentsPaths) {
                            epamComponents.put(s, componentsPath);
                        }
                        System.out.println(epamComponents);
                    }

//                    System.out.println(epamComponents.size());
                }
//                System.out.println(epamComponents.size());
//                System.out.println(allEpamComponentsNamesList.stream()
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toList()));
            }
//            System.out.println(allEpamComponentsNamesList);
//            System.out.println(epamComponents);
        } catch (IOException e) {
            e.getMessage();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return epamComponents;
    }

    private static List<String> getAllEpamComponentsNamesList(String name) {
        List<String> names = new ArrayList<>();
        if (name != null) {
            names.add(name);
        }
        return names;
    }

    private String getCountOfXmlProperties(InputStream is, String searchPattern) {
        BufferedReader br = null;
        String line;
        int count = 0;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                if (line.trim().contains(searchPattern)) {
                    System.out.println(line.trim());
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        String result = "count = " + count;
        System.out.println(result);
        return result;
    }
}
