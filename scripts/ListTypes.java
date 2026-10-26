import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Genera docs/uml-checklist.md a partir del código de src/. Es la misma lista que armaba
// scripts/list_types.py, pero en Java: así se regenera en cualquier PC del grupo que tenga el
// JDK, sin instalar Python. Uso, desde la raíz del repo: java scripts/ListTypes.java
//
// Lista, por paquete, cada clase, interfaz o enum con lo que implementa o extiende, los campos
// cuyo tipo es del proyecto (asociaciones) y los otros tipos del proyecto que usa (dependencias).
// Al final, qué paquetes importan a cuáles. El UML tiene que coincidir con esto, y esto sale del código.
public class ListTypes {

    private static final Path SRC = Path.of("src");
    private static final Path OUT = Path.of("docs", "uml-checklist.md");
    private static final Pattern DECL = Pattern.compile(
        "^(?:public\\s+)?(?:(?:final|abstract)\\s+)*(class|interface|enum)\\s+(\\w+)([^{]*)\\{", Pattern.MULTILINE);
    private static final Pattern FIELD = Pattern.compile(
        "^    (?:(?:private|protected|public|static|final)\\s+)*([A-Z]\\w*(?:<[^>\\r\\n]+>)?(?:\\[\\])*)\\s+\\w+\\s*(?:=|;)",
        Pattern.MULTILINE);
    private static final Pattern PACKAGE = Pattern.compile("^package\\s+([\\w.]+);", Pattern.MULTILINE);
    private static final Pattern IMPORT = Pattern.compile("^import\\s+([\\w.]+)\\.\\w+;", Pattern.MULTILINE);
    private static final Pattern TYPE_NAME = Pattern.compile("\\b[A-Z]\\w*\\b");
    private static final Pattern NOISE = Pattern.compile(
        "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Set<String> SKIP_FILES = Set.of("ChessCoreTests.java");
    private static final String MARKER = "## Dónde está cada patrón";

    private record Type(String pkg, String kind, String name, String text,
                        List<String> extendsList, List<String> implementsList, Set<String> imports) { }

    public static void main(String[] args) throws IOException {
        // Conserva la sección manual de patrones al regenerar el inventario.
        String previous = Files.exists(OUT)
            ? Files.readString(OUT, StandardCharsets.UTF_8).replace("\r\n", "\n") : "";
        String manual = previous.contains(MARKER) ? previous.substring(previous.indexOf(MARKER)).strip() : "";

        List<Path> files;
        try (Stream<Path> walk = Files.walk(SRC)) {
            files = walk.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.startsWith(SRC.resolve("out")))
                .filter(p -> !SKIP_FILES.contains(p.getFileName().toString()))
                .sorted().collect(Collectors.toList());
        }
        List<Type> parsed = new ArrayList<>();
        for (Path path : files) {
            String text = Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
            Matcher pkg = PACKAGE.matcher(text);
            String packageName = pkg.find() ? pkg.group(1) : "(raíz, sin paquete)";
            Matcher decl = DECL.matcher(text);
            if (!decl.find()) continue;
            String rest = decl.group(3).strip();
            Matcher ext = Pattern.compile("extends\\s+(.*?)(?=\\s+implements|$)").matcher(rest);
            Matcher impl = Pattern.compile("implements\\s+(.*)$").matcher(rest);
            Set<String> imports = new TreeSet<>();
            Matcher imp = IMPORT.matcher(text);
            while (imp.find()) imports.add(imp.group(1));
            parsed.add(new Type(packageName, decl.group(1), decl.group(2), text,
                ext.find() ? names(ext.group(1)) : List.of(), impl.find() ? names(impl.group(1)) : List.of(), imports));
        }

        Set<String> projectTypes = parsed.stream().map(Type::name).collect(Collectors.toSet());
        Set<String> projectPackages = parsed.stream().map(Type::pkg).collect(Collectors.toSet());
        List<String> lines = new ArrayList<>(Arrays.asList(
            "# Lista para dibujar el UML",
            "",
            "Generada con `java scripts/ListTypes.java` a partir del código. Si el código cambia, se vuelve a generar y el UML se revisa contra esta lista.",
            "",
            "Cómo leerla: **Implementa** es una flecha de realización (línea punteada con triángulo vacío); **Extiende** es herencia; **Tiene** son las asociaciones por campo (el diamante o la flecha simple, según si es parte de o solo referencia); **Usa** son dependencias (flecha punteada).",
            ""));
        Map<String, List<Type>> byPackage = new TreeMap<>();
        for (Type type : parsed) byPackage.computeIfAbsent(type.pkg(), k -> new ArrayList<>()).add(type);

        for (Map.Entry<String, List<Type>> entry : byPackage.entrySet()) {
            lines.add("## Paquete `" + entry.getKey() + "`");
            lines.add("");
            List<Type> types = new ArrayList<>(entry.getValue());
            types.sort((a, b) -> a.name().compareTo(b.name()));
            for (Type type : types) {
                String body = codeOnly(type.text());
                Set<String> fields = new TreeSet<>();
                Matcher field = FIELD.matcher(body);
                while (field.find()) {
                    Matcher names = TYPE_NAME.matcher(field.group(1));
                    while (names.find()) {
                        String name = names.group();
                        if (projectTypes.contains(name) && !name.equals(type.name())) fields.add(name);
                    }
                }
                Set<String> related = new LinkedHashSet<>(type.extendsList());
                related.addAll(type.implementsList());
                related.addAll(fields);
                Set<String> uses = new TreeSet<>();
                Matcher words = TYPE_NAME.matcher(body);
                while (words.find()) {
                    String word = words.group();
                    if (projectTypes.contains(word) && !word.equals(type.name()) && !related.contains(word)) uses.add(word);
                }
                lines.add("### " + type.name() + " (" + type.kind() + ")");
                if (!type.extendsList().isEmpty()) lines.add("- Extiende: " + String.join(", ", type.extendsList()));
                if (!type.implementsList().isEmpty()) lines.add("- Implementa: " + String.join(", ", type.implementsList()));
                if (!fields.isEmpty()) lines.add("- Tiene: " + String.join(", ", fields));
                if (!uses.isEmpty()) lines.add("- Usa: " + String.join(", ", uses));
                lines.add("");
            }
        }

        lines.add("## Dependencias entre paquetes");
        lines.add("");
        lines.add("| Paquete | Importa de |");
        lines.add("|---|---|");
        for (Map.Entry<String, List<Type>> entry : byPackage.entrySet()) {
            Set<String> imported = new TreeSet<>();
            for (Type type : entry.getValue()) {
                for (String imp : type.imports()) {
                    if (projectPackages.contains(imp) && !imp.equals(entry.getKey())) imported.add(imp);
                }
            }
            String list = imported.isEmpty() ? "nada del proyecto"
                : imported.stream().map(p -> "`" + p + "`").collect(Collectors.joining(", "));
            lines.add("| `" + entry.getKey() + "` | " + list + " |");
        }
        lines.add("");
        if (!manual.isEmpty()) {
            lines.add(manual);
            lines.add("");
        }
        Files.writeString(OUT, String.join("\n", lines).stripTrailing() + "\n", StandardCharsets.UTF_8);
        System.out.println("escrito " + OUT + " con " + parsed.size() + " tipos");
    }

    // Evita relaciones falsas por nombres citados solo en comentarios o textos.
    private static String codeOnly(String text) {
        Matcher matcher = NOISE.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String newlines = "\n".repeat((int) matcher.group().chars().filter(c -> c == '\n').count());
            matcher.appendReplacement(result, Matcher.quoteReplacement(newlines));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static List<String> names(String text) {
        List<String> result = new ArrayList<>();
        for (String part : text.replaceAll("<[^>]*>", "").split(",")) {
            if (!part.isBlank()) result.add(part.strip());
        }
        return result;
    }
}
