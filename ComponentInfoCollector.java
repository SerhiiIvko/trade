
import com.adobe.xfa.ut.ObjectHolder;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ComponentInfoCollector {
    public static final String CONTENT_XML_FILE_NAME = ".content.xml";
    public static final String CQ_DIALOG_DIRECTORY_NAME = "_cq_dialog";
    public static final String HTML_EXTENSION = ".html";
    public volatile int count = 0;

    public static final String COMP_NAME_GROUP = "compName";
    private static final Pattern COMPONENT_NAME_PATTERN = Pattern.compile(String.format("jcr:title=\\\"(?<%s>.+) \\(EPAM\\)\\\"", COMP_NAME_GROUP));

    public static void main(String[] argv) throws Exception {
        String pathToRoot = "ui.apps/src/main/content/jcr_root/apps/frontdoor/components/content";
        File root = new File(pathToRoot);
        if (root.exists()) {
            iterateFiles(root);
        }
    }

    private static void iterateFiles(File file) {
        String[] fileList = file.list();
        if (fileList == null) {
            return;
        }
        if (Arrays.asList(fileList).contains(CONTENT_XML_FILE_NAME)) {
            FilenameFilter filenameFilter = (dir, name) ->
                    name.equals(CONTENT_XML_FILE_NAME) || name.equals(CQ_DIALOG_DIRECTORY_NAME) || name.contains(HTML_EXTENSION);
            File[] files = file.listFiles(filenameFilter);
            if (files != null) {
                ObjectHolder<ComponentInfo> componentInfoHolder = new ObjectHolder<>();
                Stream.of(files)
                        .sorted((f1, f2) -> f1.getName().equals(CONTENT_XML_FILE_NAME)
                                ? -10
                                : f2.getName().equals(CONTENT_XML_FILE_NAME)
                                ? 10
                                : f1.getName().equals(CQ_DIALOG_DIRECTORY_NAME)
                                ? -9
                                : f2.getName().equals(CQ_DIALOG_DIRECTORY_NAME)
                                ? 9
                                : 5)
                        .forEach(f -> {
                            if (f.getName().equals(CONTENT_XML_FILE_NAME)) {
                                ComponentInfo componentInfo = handleContentFile(f);
                                if (componentInfo == null) {
                                    return;
                                }
                                componentInfoHolder.value = componentInfo;
                            } else if (f.getName().equals(CQ_DIALOG_DIRECTORY_NAME)) {
                                handleCqDialog(f, componentInfoHolder.value);
                            } else {
                                handleHtml(f, componentInfoHolder.value);
                            }
                        });
            }

        } else {
            Stream.of(fileList)
                    .map(fileName -> new File(file, fileName))
                    .filter(File::exists)
                    .forEach(ComponentInfoCollector::iterateFiles);
        }
    }

    private static void handleHtml(File f, ComponentInfo componentInfo) {
        Map<String, String> properties = new HashMap<>();
        BufferedReader reader = null;
        try {
            if (f.exists()) {
                reader = new BufferedReader(new FileReader(f.getPath()));
                String line;
                System.out.println("Lines with properties in html file: ");
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.contains("${") && !line.contains("wcmmode")) {
                        System.out.println(line);
                    }
                }
            }
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
        System.out.println();
    }

    private static void handleCqDialog(File f, ComponentInfo componentInfo) {
        Map<String, String> properties = new HashMap<>();
        BufferedReader reader = null;
        String name = null;
        f = Arrays.stream(Objects.requireNonNull(f.listFiles())).findFirst().get();
        try {
            if (f.exists() && f.getParentFile().getName().equals("_cq_dialog")) {
                reader = new BufferedReader(new FileReader(f.getPath()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.contains("name=\"./")) {
                        Pattern compile = Pattern.compile("\"(.+?)\"");
                        Matcher matcher = compile.matcher(line);
                        if (matcher.find()) {
                            name = matcher.group().replace("\"", "").replace(".", "").replace("/", "");
                            System.out.println("Component = " + f.getParentFile().getParentFile().getName() + "; property = " + name);
                        }
                    }
                }
            }
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
    }

    private static ComponentInfo handleContentFile(File f) {
        ComponentInfo componentInfo = null;
        try (Stream<String> lines = Files.lines(f.toPath())) {
            List<String> componentNames = new ArrayList<>();
            lines.forEach(line -> {
                final Matcher matcher = COMPONENT_NAME_PATTERN.matcher(line);
                if (matcher.find()) {
                    final String compName = matcher.group(COMP_NAME_GROUP);
                    componentNames.add(compName);
                    System.out.println("Component name in the code: " + f.getParentFile().getName());
                    System.out.println("Component name in the AEM: " + compName);
                    System.out.println(f.getPath());

                }

            });
            if (!componentNames.isEmpty()) {
                componentInfo = new ComponentInfo(componentNames);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return componentInfo;
    }

    private static class ComponentInfo {
        private List<String> componentNames;
        private Map<String, String> properties;

        public ComponentInfo(List<String> componentNames) {
            this.componentNames = componentNames;
        }

        public List<String> getComponentNames() {
            return componentNames;
        }

        public void setComponentNames(List<String> componentNames) {
            this.componentNames = componentNames;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }
}
