package clienteMulti;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ParaRecibir implements Runnable {
    private final DataInputStream entrada;
    private boolean autenticado = false;
 
    public ParaRecibir(Socket s) throws IOException {
        this.entrada = new DataInputStream(s.getInputStream());
    }
 
    @Override
    public void run() {
        try {
            while (true) {
                String mensaje = entrada.readUTF();
                
                if (mensaje.contains("Registro exitoso") || 
                    mensaje.contains("Inicio de sesion exitoso")) {
                    autenticado = true;
                }
                
                System.out.println(mensaje);
            }
        } catch (IOException e) {
            System.out.println("\n Conexion con el servidor perdida.");
            System.out.println("El servidor se ha desconectado o hay problemas de red");
        } finally {
            try { 
                entrada.close();
            } catch (IOException ignored) {}
        }
    }
    
    public boolean isAutenticado() {
        return autenticado;
    }
}