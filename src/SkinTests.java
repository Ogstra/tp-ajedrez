import adaptadores.FileSkinCatalog;
import adaptadores.FileSkinStore;
import puertos.IAccountStore;
import puertos.ISkinStore;
import skins.Skin;
import skins.SkinService;
import usuarios.AccountService;
import usuarios.Result;
import usuarios.Score;
import usuarios.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tests de las skins. Mismo runner propio que los otros: Arrange, Act, Assert y salida [OK] / [FAIL].
public class SkinTests {

    private static int passed = 0;
    private static int failed = 0;

    private static final Skin CLASSIC = new Skin("clasico", "Clásico", 0, "EEEED2", "769656", "piezas");
    private static final Skin WOOD = new Skin("madera", "Madera", 10, "F0D9B5", "B58863", "piezas");
    private static final Skin ICE = new Skin("hielo", "Hielo", 25, "DEE3E6", "8CA2AD", "piezas");

    public static void main(String[] args) throws IOException {
        testGuestOnlyHasTheFreeSkin();
        testSkinUnlocksWhenPointsReachIt();
        testLockedSkinCannotBeSelected();
        testSelectedSkinIsUsed();
        testSkinStopsBeingUsedIfPointsDrop();
        testGuestChoiceLastsTheSession();
        testFileStoreRemembersEachUser();
        testCatalogFileIsRead();
        testCatalogWithoutFileHasTheClassicSkin();

        System.out.println("\n" + passed + " pasados, " + failed + " fallados.");
        System.exit(failed > 0 ? 1 : 0);
    }

    // ---------- ayudas ----------

    private static void assertTrue(String name, boolean condition, String detail) {
        if (condition) {
            passed++;
            System.out.println("[OK]   " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name + " -> " + detail);
        }
    }

    // Stub: las cuentas en memoria, sin archivos.
    private static class InMemoryAccountStore implements IAccountStore {
        private List<Score> scores = new ArrayList<>();
        @Override public List<Score> loadAll() { return new ArrayList<>(scores); }
        @Override public void saveAll(List<Score> scores) { this.scores = new ArrayList<>(scores); }
    }

    // Stub: la skin elegida por cada usuario, en memoria.
    private static class InMemorySkinStore implements ISkinStore {
        private final Map<Integer, String> chosen = new HashMap<>();
        @Override public String load(int userId) { return chosen.get(userId); }
        @Override public void save(int userId, String skinId) { chosen.put(userId, skinId); }
    }

    private static AccountService newAccounts() {
        return new AccountService(new InMemoryAccountStore());
    }

    private static SkinService newSkins(AccountService accounts) {
        return new SkinService(Arrays.asList(CLASSIC, WOOD, ICE), accounts, new InMemorySkinStore());
    }

    // Cada victoria vale 3 puntos.
    private static void win(AccountService accounts, User user, int times) {
        for (int i = 0; i < times; i++) accounts.record(user, Result.WIN);
    }

    // ---------- tests ----------

    private static void testGuestOnlyHasTheFreeSkin() {
        // Arrange
        SkinService skins = newSkins(newAccounts());

        // Act + Assert
        assertTrue("testGuestOnlyHasTheFreeSkin",
            skins.isUnlocked(CLASSIC, null) && !skins.isUnlocked(WOOD, null) && skins.selected(null).equals(CLASSIC),
            "un invitado tiene 0 puntos: solo la skin de siempre");
    }

    private static void testSkinUnlocksWhenPointsReachIt() {
        // Arrange
        AccountService accounts = newAccounts();
        User ana = accounts.register("Ana");
        SkinService skins = newSkins(accounts);

        // Act: 3 victorias son 9 puntos (no alcanza para Madera); la cuarta suma 12.
        win(accounts, ana, 3);
        boolean lockedAt9 = !skins.isUnlocked(WOOD, ana);
        win(accounts, ana, 1);

        // Assert
        assertTrue("testSkinUnlocksWhenPointsReachIt",
            lockedAt9 && skins.isUnlocked(WOOD, ana) && !skins.isUnlocked(ICE, ana),
            "Madera pide 10 puntos: bloqueada con 9, desbloqueada con 12; Hielo (25) sigue bloqueada");
    }

    private static void testLockedSkinCannotBeSelected() {
        // Arrange
        AccountService accounts = newAccounts();
        User ana = accounts.register("Ana");
        SkinService skins = newSkins(accounts);

        // Act
        boolean rejected = false;
        try {
            skins.select(ana, ICE);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        // Assert
        assertTrue("testLockedSkinCannotBeSelected", rejected && skins.selected(ana).equals(CLASSIC),
            "elegir una skin bloqueada se rechaza y sigue la de siempre");
    }

    private static void testSelectedSkinIsUsed() {
        // Arrange
        AccountService accounts = newAccounts();
        User ana = accounts.register("Ana");
        SkinService skins = newSkins(accounts);
        win(accounts, ana, 4);

        // Act
        skins.select(ana, WOOD);

        // Assert
        assertTrue("testSelectedSkinIsUsed", skins.selected(ana).equals(WOOD), "Ana eligió Madera");
    }

    private static void testSkinStopsBeingUsedIfPointsDrop() {
        // Arrange: Ana llega a 12 puntos y elige Madera.
        AccountService accounts = newAccounts();
        User ana = accounts.register("Ana");
        SkinService skins = newSkins(accounts);
        win(accounts, ana, 4);
        skins.select(ana, WOOD);

        // Act: se deshace una victoria y quedan 9 puntos.
        accounts.revert(ana, Result.WIN);
        Skin withNine = skins.selected(ana);
        win(accounts, ana, 1);

        // Assert: con 9 vuelve a la de siempre; al recuperar los puntos, vuelve Madera sola.
        assertTrue("testSkinStopsBeingUsedIfPointsDrop",
            withNine.equals(CLASSIC) && skins.selected(ana).equals(WOOD),
            "con 9 puntos no alcanza para Madera; con 12 vuelve la elegida");
    }

    private static void testGuestChoiceLastsTheSession() {
        // Arrange: un catálogo con dos skins gratis.
        Skin freeToo = new Skin("gris", "Gris", 0, "DDDDDD", "888888", "piezas");
        SkinService skins = new SkinService(Arrays.asList(CLASSIC, freeToo), newAccounts(), new InMemorySkinStore());

        // Act
        skins.select(null, freeToo);

        // Assert
        assertTrue("testGuestChoiceLastsTheSession", skins.selected(null).equals(freeToo),
            "un invitado puede elegir entre las gratis mientras dure la sesión");
    }

    private static void testFileStoreRemembersEachUser() throws IOException {
        // Arrange
        Path file = Files.createTempDirectory("skins").resolve("skins.csv");
        new FileSkinStore(file).save(1, "madera");
        new FileSkinStore(file).save(2, "hielo");

        // Act: otro objeto lee el mismo archivo, y Ana cambia de skin.
        FileSkinStore store = new FileSkinStore(file);
        store.save(1, "clasico");

        // Assert
        assertTrue("testFileStoreRemembersEachUser",
            "clasico".equals(store.load(1)) && "hielo".equals(store.load(2)) && store.load(3) == null,
            "cada usuario conserva su skin y cambiar una no pisa la otra");
    }

    private static void testCatalogFileIsRead() throws IOException {
        // Arrange: comentarios, una línea mal formada y un color que no es hexadecimal.
        Path file = Files.createTempDirectory("catalogo").resolve("skins.csv");
        Files.write(file, Arrays.asList(
            "# comentario",
            "clasico;Clásico;0;EEEED2;769656;piezas",
            "rota;Sin puntos",
            "fea;Fea;5;ZZZZZZ;000000;piezas",
            "madera;Madera;10;F0D9B5;B58863;skins/madera"), StandardCharsets.UTF_8);

        // Act
        List<Skin> skins = FileSkinCatalog.load(file);

        // Assert
        assertTrue("testCatalogFileIsRead",
            skins.size() == 2 && skins.get(0).getName().equals("Clásico")
                && skins.get(1).getRequiredPoints() == 10 && skins.get(1).getPiecesFolder().equals("skins/madera"),
            "se leen las dos skins válidas, con acentos y su carpeta");
    }

    private static void testCatalogWithoutFileHasTheClassicSkin() throws IOException {
        // Act
        List<Skin> skins = FileSkinCatalog.load(Files.createTempDirectory("vacio").resolve("no-existe.csv"));

        // Assert
        assertTrue("testCatalogWithoutFileHasTheClassicSkin",
            skins.size() == 1 && skins.get(0).getRequiredPoints() == 0,
            "sin catálogo queda la skin de siempre");
    }
}
