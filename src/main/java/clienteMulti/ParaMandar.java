package clienteMulti;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ParaMandar implements Runnable {

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
            
            System.out.println("-------- BIENVENIDO AL CHAT --------");
            
            System.out.println("Tienes 3 mensajes gratis.");
            System.out.println("\n Comandos:");
            System.out.println("   /registro <usuario> <password>");
            System.out.println("   /login <usuario> <password>");
            System.out.println("   /usuarios - Ver usuarios registrados");    
            System.out.println("   /online - Ver quien esta conectado");       
            System.out.println("   /desbloquear <usuario>");
            System.out.println("   /bloqueados");
            System.out.println("\n  Juego del gatito");
            System.out.println("   /gato <usuario - Invitar a jugar> ");
            System.out.println("   /aceptar - Aceptar invitacion");
            System.out.println("   /rechazar - Rechazar invitacion");
            System.out.println("   /jugar <fila> <columna> - Hacer jugada (0-2)");
            System.out.println("   /tablero - Ver tablero actual");
            System.out.println("   /rendirse - Abandonar partida");
            System.out.println("   /ayuda - Ver lista de comandos");
            System.out.println("   salir - Cerrar conexion");
            
            System.out.println("---------------------------------------\n");

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
                        || mensaje.equals("/tablero") || mensaje.equals("/rendirse")) {
                    salida.writeUTF(mensaje);
                    salida.flush();
                    continue;
                }

                if ("salir".equalsIgnoreCase(mensaje)) {
                    salida.writeUTF(mensaje);
                    salida.flush();
                    System.out.println("\n Cerrando conexion...");
                    socket.close();
                    break;
                }

                if (paraRecibir != null) {
                    autenticado = paraRecibir.isAutenticado();
                }

                if (!autenticado && mensajesEnviados >= 3) {
                    System.out.println("\n Has alcanzado el limite de 3 mensajes");
                    System.out.println("Usa: /registro <usuario> <password>");
                    System.out.println("O:   /login <usuario> <password>\n");
                    continue;
                }

                salida.writeUTF(mensaje);
                salida.flush();

                if (!autenticado) {
                    mensajesEnviados++;
                    int restantes = 3 - mensajesEnviados;
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
