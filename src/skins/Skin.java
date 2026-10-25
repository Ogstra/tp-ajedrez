package skins;

// Value object inmutable: una skin cambia las piezas y los colores del tablero, y se desbloquea
// al llegar a un puntaje del ranking. Los colores van como texto hexadecimal ("EEEED2") para que
// esta clase no dependa de Swing; la ventana los convierte al dibujar.
public final class Skin {
    private final String id;
    private final String name;
    private final int requiredPoints;
    private final String lightSquare;
    private final String darkSquare;
    private final String piecesFolder; // carpeta de las 12 imágenes (wK.png, bQ.png...), dentro de src/assets

    public Skin(String id, String name, int requiredPoints, String lightSquare, String darkSquare, String piecesFolder) {
        this.id = id;
        this.name = name;
        this.requiredPoints = requiredPoints;
        this.lightSquare = lightSquare;
        this.darkSquare = darkSquare;
        this.piecesFolder = piecesFolder;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getRequiredPoints() { return requiredPoints; }
    public String getLightSquare() { return lightSquare; }
    public String getDarkSquare() { return darkSquare; }
    public String getPiecesFolder() { return piecesFolder; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Skin && id.equals(((Skin) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
