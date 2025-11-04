package servidorMulti;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServidorMulti {
    public static HashMap<String, unCliente> clientes = new HashMap<>();
 
    public static void main(String[] args) {
        DatabaseManager.inicializarBaseDatos();
        
        int puerto = 8080;
        int contador = 0;
 
        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            
            System.out.println("-------- SERVIDOR CHAT INICIADO --------");
            System.out.println("Puerto: " + puerto);
            System.out.println("Usuarios: " + DatabaseManager.contarUsuarios());
            System.out.println("Esperando conexiones...\n");
 
            while (true) {
                Socket socket = servidorSocket.accept();
 
                String idCliente = Integer.toString(contador);
                unCliente uncliente = new unCliente(socket, idCliente);
                Thread hilo = new Thread(uncliente);
 
                clientes.put(idCliente, uncliente);
                hilo.start();
 
                System.out.println("[CONEXION] Cliente #" + contador);
                contador++;
            }
        } catch (IOException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }
    
    // METODOS PARA ACCEDER A CLIENTES
    public static HashMap<String, unCliente> getClientes() {
        return clientes;
    }
    
    public static void removeCliente(String id) {
        clientes.remove(id);
        System.out.println("[SERVIDOR] Cliente #" + id + " removido");
    }
}