package clienteMulti;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ParaMandar implements Runnable {

    public static final int MENSAJES_GRATIS_MAXIMOS = 3;
    private final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    private final DataOutputStream salida;
    private final Socket socket;
    private int mensajesEnviados = 0;
    private boolean autenticado = false;
    private ParaRecibir paraRecibir;

    public ParaMandar(Socket s) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
    }

    public void setParaRecibir(ParaRecibir pr) {
        this.paraRecibir = pr;
    }

    @Override
    public void run() {
        try {

            System.out.println("""
                        -------- BIENVENIDO AL CHAT --------

                        Tienes 3 mensajes gratis.

                         Comandos:
                           /registro <usuario> <password>
                           /login <usuario> <password>
                           /usuarios - Ver usuarios registrados
                           /online - Ver quien esta conectado
                           /desbloquear <usuario>
                           /bloqueados
                               
                          Grupos:
                          /creargrupo <nombre>
                          /unirgrupo <nombre>
                          /entrargrupo <nombre>
                          /grupos - Ver todos los grupos
                          /misgrupos - Ver mis grupos

                          Juego del gatito
                           /gato <usuario> - Invitar a jugar
                           /aceptar - Aceptar invitacion
                           /rechazar - Rechazar invitacion
                           /jugar <fila> <columna> - Hacer jugada (0-2)
                           /tablero - Ver tablero actual
                           /rendirse - Abandonar partida

                          Estadisticas:
                           /ranking - Ver ranking general
                           /stats - <usuario1> <usuario2> - Comparar jugadores
                           /ayuda - Ver lista de comandos
                           /salir - Cerrar conexion
                        ---------------------------------------
                        """.formatted(MENSAJES_GRATIS_MAXIMOS));

            while (true) {
                String mensaje = teclado.readLine();
                if (mensaje == null) {
                    break;
                }

                if (mensaje.startsWith("/registro ") || mensaje.startsWith("/login ")
                        || mensaje.startsWith("/bloquear ") || mensaje.startsWith("/desbloquear ")
                        || mensaje.equals("/bloqueados") || mensaje.equals("/ayuda")
                        || mensaje.equals("/usuarios") || mensaje.equals("/online")
                        || mensaje.startsWith("/gato ") || mensaje.equals("/aceptar")
                        || mensaje.equals("/rechazar") || mensaje.startsWith("/jugar")
                        || mensaje.equals("/tablero") || mensaje.equals("/rendirse")
                        || mensaje.equals("/ranking") || mensaje.startsWith("/stats")
                        || mensaje.startsWith("/creargrupo") || mensaje.startsWith("/unirgrupo")
                        || mensaje.startsWith("/entrargrupo") || mensaje.startsWith("/eliminargrupo")
                        || mensaje.equals("/grupos") || mensaje.equals("/misgrupos")
                        || mensaje.startsWith("/miembros")){
                    salida.writeUTF(mensaje);
                    salida.flush();
                    continue;
                }

                if ("/salir".equalsIgnoreCase(mensaje)) {
                    salida.writeUTF(mensaje);
                    salida.flush();
                    System.out.println("\n Cerrando conexion...");
                    socket.close();
                    break;
                }

                if (paraRecibir != null) {
                    autenticado = paraRecibir.isAutenticado();
                }

                if (!autenticado && mensajesEnviados >= MENSAJES_GRATIS_MAXIMOS) {
                    System.out.println("\n Has alcanzado el limite de "+ MENSAJES_GRATIS_MAXIMOS +" mensajes");
                    System.out.println("Usa: /registro <usuario> <password>");
                    System.out.println("O:   /login <usuario> <password>\n");
                    continue;
                }

                salida.writeUTF(mensaje);
                salida.flush();

                if (!autenticado) {
                    mensajesEnviados++;
                    int restantes = MENSAJES_GRATIS_MAXIMOS - mensajesEnviados;
                    if (restantes > 0) {
                        System.out.println(" (Te quedan " + restantes + " mensajes)");
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
