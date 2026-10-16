#!/usr/bin/env python3
"""Genera docs/uml-checklist.md a partir del código de src/.

Lista, por paquete, cada clase, interfaz o enum con: lo que implementa o
extiende, los campos cuyo tipo es del proyecto (asociaciones) y los otros
tipos del proyecto que usa (dependencias). Al final, qué paquetes importan
a cuáles. Es la lista para dibujar el UML: el diagrama tiene que coincidir
con esto, y esto sale del código.
"""
import pathlib
import re

SRC = pathlib.Path("src")
OUT = pathlib.Path("docs/uml-checklist.md")
DECL = re.compile(r"^(?:public\s+)?(?:(?:final|abstract)\s+)*(class|interface|enum)\s+(\w+)([^{]*)\{", re.M)
FIELD = re.compile(r"^    (?:(?:private|protected|public|static|final)\s+)*([A-Z]\w*(?:<[^>\r\n]+>)?(?:\[\])*)\s+\w+\s*(?:=|;)", re.M)
PACKAGE = re.compile(r"^package\s+([\w.]+);", re.M)
IMPORT = re.compile(r"^import\s+([\w.]+)\.\w+;", re.M)
SKIP_FILES = {"ChessCoreTests.java"}


def code_only(text):
    # Evita relaciones falsas por nombres citados solo en comentarios o textos.
    tokens = re.compile(r"""(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|/\*.*?\*/|//[^\n]*)""", re.DOTALL)
    return tokens.sub(lambda match: "\n" * match.group(0).count("\n"), text)


def names(text):
    text = re.sub(r"<[^>]*>", "", text)
    return [part.strip() for part in text.split(",") if part.strip()]


def main():
    files = [p for p in sorted(SRC.rglob("*.java")) if p.name not in SKIP_FILES]
    parsed = []
    for path in files:
        text = path.read_text(encoding="utf-8")
        package_match = PACKAGE.search(text)
        package = package_match.group(1) if package_match else "(raíz, sin paquete)"
        match = DECL.search(text)
        if not match:
            continue
        kind, name, rest = match.group(1), match.group(2), match.group(3)
        extends = re.search(r"extends\s+(.*?)(?=\s+implements|$)", rest.strip())
        implements = re.search(r"implements\s+(.*)$", rest.strip())
        parsed.append({
            "package": package, "kind": kind, "name": name, "text": text,
            "extends": names(extends.group(1)) if extends else [],
            "implements": names(implements.group(1)) if implements else [],
            "imports": sorted(set(IMPORT.findall(text))),
        })

    project_types = {item["name"] for item in parsed}
    lines = [
        "# Lista para dibujar el UML",
        "",
        "Generada con `python3 scripts/list_types.py` a partir del código. Si el código cambia, se vuelve a generar y el UML se revisa contra esta lista.",
        "",
        "Cómo leerla: **Implementa** es una flecha de realización (línea punteada con triángulo vacío); **Extiende** es herencia; **Tiene** son las asociaciones por campo (el diamante o la flecha simple, según si es parte de o solo referencia); **Usa** son dependencias (flecha punteada).",
        "",
    ]
    by_package = {}
    for item in parsed:
        by_package.setdefault(item["package"], []).append(item)

    for package in sorted(by_package):
        lines += ["## Paquete `" + package + "`", ""]
        for item in sorted(by_package[package], key=lambda i: i["name"]):
            body = code_only(item["text"])
            fields = []
            for match in FIELD.finditer(body):
                for type_name in re.findall(r"\b[A-Z]\w*\b", match.group(1)):
                    if type_name and type_name in project_types and type_name != item["name"]:
                        fields.append(type_name)
            words = set(re.findall(r"\b[A-Z]\w*\b", body))
            related = set(item["extends"]) | set(item["implements"]) | set(fields)
            uses = sorted(w for w in words if w in project_types and w != item["name"] and w not in related)
            lines.append("### " + item["name"] + " (" + item["kind"] + ")")
            if item["extends"]:
                lines.append("- Extiende: " + ", ".join(item["extends"]))
            if item["implements"]:
                lines.append("- Implementa: " + ", ".join(item["implements"]))
            if fields:
                lines.append("- Tiene: " + ", ".join(sorted(set(fields))))
            if uses:
                lines.append("- Usa: " + ", ".join(uses))
            lines.append("")

    lines += ["## Dependencias entre paquetes", ""]
    lines += ["| Paquete | Importa de |", "|---|---|"]
    for package in sorted(by_package):
        imported = set()
        for item in by_package[package]:
            for imp in item["imports"]:
                if imp in {i["package"] for i in parsed} and imp != package:
                    imported.add(imp)
        lines.append("| `" + package + "` | " + (", ".join("`" + i + "`" for i in sorted(imported)) or "nada del proyecto") + " |")
    lines.append("")
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("escrito " + str(OUT) + " con " + str(len(parsed)) + " tipos")


main()
