package servidorMulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServidorMulti {

     static HashMap<String, unCliente> clientes = new HashMap<>();
 
    public static void main(String[] args) {
        int puerto = 8080;
        int contador = 0;
 
        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);
 
            while (true) {
                Socket socket = servidorSocket.accept();
 
                String idCliente = Integer.toString(contador);
                unCliente uncliente = new unCliente(socket, idCliente);
                Thread hilo = new Thread(uncliente);
 
                clientes.put(idCliente, uncliente);
 
                hilo.start();
 
                System.out.println("se conecto el chango #" + contador);
 
                contador++;
            }
        } catch (IOException e) {
            System.out.println("Error en servidor: " + e.getMessage());
        }
    }
}
