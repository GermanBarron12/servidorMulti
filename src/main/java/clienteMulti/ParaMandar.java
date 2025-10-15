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
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║      BIENVENIDO AL CHAT                  ║");
            System.out.println("╚══════════════════════════════════════════╝");
            System.out.println("Tienes 3 mensajes gratis.");
            System.out.println("\n📋 Comandos:");
            System.out.println("   /registro <usuario> <password>");
            System.out.println("   /login <usuario> <password>");
            System.out.println("   /bloquear <usuario>");
            System.out.println("   /desbloquear <usuario>");
            System.out.println("   /bloqueados");
            System.out.println("   /ayuda");
            System.out.println("   salir\n");
            
            while (true) {
                String mensaje = teclado.readLine();
                if (mensaje == null) break;
 
                if (mensaje.startsWith("/registro ") || mensaje.startsWith("/login ") ||
                    mensaje.startsWith("/bloquear ") || mensaje.startsWith("/desbloquear ") ||
                    mensaje.equals("/bloqueados") || mensaje.equals("/ayuda")) {
                    salida.writeUTF(mensaje);
                    salida.flush();
                    continue;
                }
                
                if ("salir".equalsIgnoreCase(mensaje)) {
                    salida.writeUTF(mensaje);
                    salida.flush();
                    System.out.println("\n👋 Cerrando conexion...");
                    socket.close();
                    break;
                }
                
                if (paraRecibir != null) {
                    autenticado = paraRecibir.isAutenticado();
                }
                
                if (!autenticado && mensajesEnviados >= 3) {
                    System.out.println("\n⚠️ Has alcanzado el limite de 3 mensajes");
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
                        System.out.println("💡 (Te quedan " + restantes + " mensajes)");
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("❌ Error: " + ex.getMessage());
        }
    }
}