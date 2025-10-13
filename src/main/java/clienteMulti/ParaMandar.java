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
    private ParaRecibir paraRecibir; // Referencia al hilo receptor
 
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
            System.out.println("=== BIENVENIDO AL CHAT ===");
            System.out.println("Tienes 3 mensajes gratis. Despues tienes que registrarte o iniciar sesion.");
            System.out.println("Comandos: /registro <usuario> <password> | /login <usuario> <password> | salir");
            System.out.println("==========================\n");
            
            while (true) {
                String mensaje = teclado.readLine();
                if (mensaje == null) break;
 
                // Verificar comandos especiales
                if (mensaje.startsWith("/registro ") || mensaje.startsWith("/login ")) {
                    salida.writeUTF(mensaje);
                    salida.flush();
                    continue;
                }
                
                if ("salir".equalsIgnoreCase(mensaje)) {
                    salida.writeUTF(mensaje);
                    salida.flush();
                    System.out.println("Cerrando conexion...");
                    socket.close();
                    break;
                }
                
                // Verificar autenticación desde ParaRecibir
                if (paraRecibir != null) {
                    autenticado = paraRecibir.isAutenticado();
                }
                
                // Verificar si puede enviar mensajes
                if (!autenticado && mensajesEnviados >= 3) {
                    System.out.println("\n️  Has alcanzado el limite de 3 mensajes gratis");
                    System.out.println("Por favor, registrate o inicia sesion para continuar enviando mensajes.");
                    System.out.println("Usa: /registro <usuario> <password> o /login <usuario> <password>");
                    System.out.println("Puedes seguir viendo los mensajes de otros usuarios.\n");
                    continue;
                }
                
                // Enviar mensaje normal
                salida.writeUTF(mensaje);
                salida.flush();
                
                if (!autenticado) {
                    mensajesEnviados++;
                    int restantes = 3 - mensajesEnviados;
                    if (restantes > 0) {
                        System.out.println("(Te quedan " + restantes + " mensajes gratuitos)");
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Error en ParaMandar: " + ex.getMessage());
        }
    }
}