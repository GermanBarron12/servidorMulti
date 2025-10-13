package servidorMulti;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServidorMulti {
    static HashMap<String, unCliente> clientes = new HashMap<>();
    static HashMap<String, String> usuarios = new HashMap<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
 
    public static void main(String[] args) {
        // Cargar usuarios existentes al iniciar el servidor
        cargarUsuarios();
        
        int puerto = 8080;
        int contador = 0;
 
        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);
            System.out.println("Usuarios cargados: " + usuarios.size());
 
            while (true) {
                Socket socket = servidorSocket.accept();
 
                String idCliente = Integer.toString(contador);
                unCliente uncliente = new unCliente(socket, idCliente);
                Thread hilo = new Thread(uncliente);
 
                clientes.put(idCliente, uncliente);
 
                hilo.start();
 
                System.out.println("Se conecto el cliente #" + contador);
 
                contador++;
            }
        } catch (IOException e) {
            System.out.println("Error en servidor: " + e.getMessage());
        }
    }
    
    // Cargar usuarios desde el archivo
    private static void cargarUsuarios() {
        File archivo = new File(ARCHIVO_USUARIOS);
        
        if (!archivo.exists()) {
            System.out.println("Archivo de usuarios no existe. Se creara uno nuevo.");
            return;
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue; // Ignorar líneas vacías o comentarios
                }
                
                String[] partes = linea.split(":");
                if (partes.length == 2) {
                    usuarios.put(partes[0], partes[1]);
                }
            }
            System.out.println("Usuarios cargados desde " + ARCHIVO_USUARIOS);
        } catch (IOException e) {
            System.out.println("Error al cargar usuarios: " + e.getMessage());
        }
    }
    
    // Guardar un nuevo usuario en el archivo
    public static synchronized void guardarUsuario(String usuario, String password) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            bw.write(usuario + ":" + password);
            bw.newLine();
            System.out.println("Usuario guardado: " + usuario);
        } catch (IOException e) {
            System.out.println("Error al guardar usuario: " + e.getMessage());
        }
    }
}