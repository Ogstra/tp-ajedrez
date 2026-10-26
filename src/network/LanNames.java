package network;

// Los nombres que llegan por la red los escribió otra PC: son entrada no confiable (Unidad 5,
// "la salida del otro lado se valida"). Se limpian antes de mostrarlos: sin caracteres de control,
// sin caracteres de formato invisibles (como los que invierten la dirección del texto y armarían
// un nombre engañoso), sin pares sustitutos (emojis; así el corte por largo no parte uno a la
// mitad), sin "<" ni ">" (Swing interpreta como HTML un texto que empieza con "<html>"), sin ";"
// (separa los campos del aviso) y con el mismo largo máximo que un nombre de usuario.
public final class LanNames {

    static final int MAX_LENGTH = 20;
    private static final String UNKNOWN = "rival";

    private LanNames() { }

    public static String clean(String name) {
        if (name == null) return UNKNOWN;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length() && result.length() < MAX_LENGTH; i++) {
            char c = name.charAt(i);
            if (Character.isISOControl(c) || Character.getType(c) == Character.FORMAT || Character.isSurrogate(c) || c == '<' || c == '>' || c == ';') continue;
            result.append(c);
        }
        String text = result.toString().trim();
        return text.isEmpty() ? UNKNOWN : text;
    }
}
