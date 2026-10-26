# Jugar por LAN

Dos personas en PCs distintas de la misma red (la misma casa, el mismo wifi o el hotspot de un celular) juegan una partida sin escribir ninguna IP: una crea la partida y la otra la elige de una lista. Si la persona entró con su usuario, el resultado suma a su ranking; como invitado no suma.

Todo está hecho con lo que trae Java (`java.net` y `java.io`), sin librerías. El núcleo (`core/`) no cambió: la red es otro adaptador, igual que la consola, la ventana y la IA.

## El flujo

```
PC de Ana (crea la partida, blancas)              PC de Beto (se une, negras)
-------------------------------------             ---------------------------
Crear partida
  abre un puerto TCP libre (lo elige el sistema, por ejemplo 52817) y espera
  LanAnnouncer, cada 1 s por UDP 47123:
    "AJEDREZ;4821;Ana;52817" ---- broadcast ---->  LanBrowser escucha el puerto 47123
                                                     arma la lista: "Partida de Ana"
                                                   toca "Partida de Ana"
                             <---- conexión TCP --   LanConnection.connect(IP de Ana, 52817)
  deja de anunciar
  "HOLA;Ana"                 <------------------>   "HOLA;Beto"
  juega e2 e4  "e2 e4"       ------------------->   la aplica con tryMove
               "e7 e5"       <-------------------   juega e7 e5
  ...
```

## Las clases (paquete `network/`)

| Clase | Qué hace |
|---|---|
| `LanAnnouncer` | Mientras se espera rival, avisa a toda la red cada segundo, por UDP broadcast, que hay una partida: `AJEDREZ;<id>;<nombre>;<puerto>`. |
| `LanBrowser` | Escucha esos avisos y arma la lista de partidas. La IP sale del paquete recibido. Una partida que no avisa por 3 segundos sale de la lista. |
| `LanGame` | Una partida de la lista: identificador, nombre de quien la creó, IP y puerto. |
| `LanLobby` | Lo que pasa antes de la partida: buscar partidas, crear una (abre el puerto libre y anuncia) y esperar rival, o unirse a una de la lista. Cada espera va en su hilo. |
| `LanConnection` | El canal TCP entre las dos PCs: saludo (`HOLA;nombre`) y una línea de texto por jugada. `CHAU` o el cierre del canal es que el rival se fue. Corta las líneas de más de 128 caracteres y espera el saludo hasta 5 segundos. |
| `LanNames` | Limpia los nombres que llegan por la red: sin caracteres de control, sin `<`, `>` ni `;`, hasta 20 caracteres. |
| `LanOpponent` | El rival de la otra PC, para la partida. Ver abajo. |

En la ventana, `LanPanel` es la pantalla "Jugar por LAN" (Crear partida, la lista y la espera de rival). Solo muestra y reacciona a los botones: le pide todo al `LanLobby` que recibe por constructor.

## Puertos

- **La partida (TCP) no tiene puerto fijo.** Quien la crea abre el socket en el puerto 0, el sistema elige uno libre y ese número viaja en el aviso. La primera versión usaba el 5000, que en macOS ocupa el receptor de AirPlay (y el 7000 también): en una Mac no se podía crear la partida.
- **Los avisos (UDP) usan el 47123**, porque las dos PCs tienen que coincidir en algo para encontrarse. Si en alguna red está ocupado, se cambia al arrancar con `-Dajedrez.lan.port=otro`, el mismo en las dos PCs.
- Para buscar con dos ventanas en la misma PC, el socket de los avisos se abre con `SO_REUSEADDR` y, en macOS y Linux, también con `SO_REUSEPORT` (Windows no lo tiene ni lo necesita).

## Lo que llega por la red no es confiable

Es la misma idea del ejemplo de la cátedra con la IA (Unidad 5): el otro lado propone y el núcleo decide.

- El nombre del rival y el de cada partida de la lista se limpian (`LanNames`) antes de mostrarlos. Swing interpreta como HTML un texto que empieza con `<html>`; además la barra de estado escapa el texto cuando lo pasa a dos filas.
- Una línea de más de 128 caracteres se trata como desconexión: no se guarda sin límite en memoria.
- Una jugada mal formada, o que no está entre las legales de la partida, cierra la conexión y la ventana lo muestra como desconexión. El juego no se traba esperando una jugada que nunca va a ser válida.
- Un aviso con un puerto fuera de rango o con otra forma se ignora, y la lista no pasa de 20 partidas.
- Quien se conecta y no saluda en 5 segundos no deja trabada la espera.

## Por qué UDP para la lista y TCP para la partida

- **UDP** manda mensajes sueltos y permite *broadcast*: un mensaje a todas las PCs de la red sin saber quiénes son. No garantiza que lleguen, pero no importa: el aviso se repite cada segundo.
- **TCP** es una conexión: lo que se manda llega completo, en orden y sin repetirse, y si el otro se va se nota. Es lo que hace falta para las jugadas.

Una PC puede tener varias placas de red (wifi, cable, VPN, máquinas virtuales). Por eso el aviso se manda a la dirección de broadcast de cada una, y también a la propia PC para poder probar con dos ventanas en la misma máquina. El mismo aviso puede llegar varias veces; el identificador de la partida evita que aparezca repetida.

## Cómo se engancha con el juego

`LanOpponent` implementa tres interfaces que ya existían:

| Interfaz | Para qué |
|---|---|
| `IOpponent` (Strategy) | `chooseMove()` espera la línea del rival y la convierte en jugada. Para la ventana es un oponente más, como los niveles de la IA: en vez de pensar, espera. Usa el mismo hilo aparte que la IA, así la ventana no se congela. |
| `IMoveListener` (Observer) | Se entera de cada jugada propia y la manda: `e2 e4`. El enroque se avisa sin casillas, pero el rey siempre mueve dos columnas, así que se reconstruye y se manda como jugada del rey. |
| `IPromotionChooser` | Si corona el rival, devuelve la pieza que vino en el mensaje (`e7 e8 Q`). Si corona la persona, le pregunta y manda la pieza junto con la jugada. |

Cada PC tiene su propio `ChessGame` y aplica las jugadas del rival con `tryMove`, con las mismas reglas. Por la red solo viajan jugadas, nunca el tablero: como las dos partidas arrancan iguales y reciben las mismas jugadas en el mismo orden, terminan iguales.

`MainSwing` arma la partida por LAN igual que contra la IA: agrega el `LanOpponent` como listener y como elector de coronación, y lo pone como oponente en un `OpponentController`.

## Puntos

Cada PC acredita a su propio usuario en su propio `data/usuarios.csv`, con el `ScoreRecorder` de siempre. Si alguien juega como invitado, no suma.

## Límites

- Sin Deshacer ni Rehacer: habría que pedirle permiso al rival.
- Si el rival se va (cierra el juego, sale de la partida o se corta la red), se muestra "<nombre> se desconectó." y se vuelve con Menú. No hay reconexión.
- Solo en la misma red local. Por internet haría falta abrir puertos en el router.
- La primera vez Windows (o el firewall de macOS) pregunta si deja que Java use la red: hay que permitirlo en las dos PCs (redes privadas).
- Algunas redes (wifi de facultad, redes de invitados) no dejan que dos PCs se hablen: ahí la lista queda vacía.
- Solo en la ventana; la consola sigue con 1 contra 1 en la misma PC y contra la IA.

## Pruebas

- `src/LanTests.java` (corre en `./scripts/test.sh`): conexión y saludo, el mate del loco por la red con los dos tableros iguales, coronación a caballo y enroque viajando por la red, desconexión, un aviso que aparece en la lista, el puerto de avisos que no es de AirPlay, una partida creada con el lobby en un puerto libre y unida desde la lista, y los datos inválidos: línea que no es jugada, jugada ilegal, línea demasiado larga y nombre con HTML. Las dos "PCs" son dos partidas en el mismo programa, conectadas por `localhost`.
- `scripts/SwingIntegrationTests.java`: dos ventanas reales; una crea la partida, la otra la elige de la lista, juegan el mate del loco con clicks y a Ana le suma la victoria.
