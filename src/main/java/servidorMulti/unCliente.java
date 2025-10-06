package servidorMulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class unCliente implements Runnable {
  final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;
 
    unCliente(Socket s) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }
 
    @Override
    public void run() {
        while (true) {
            try {
                String mensaje = entrada.readUTF();
 
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    String aQuien = partes[0].substring(1);
                    unCliente cliente = ServidorMulti.clientes.get(aQuien);
                    cliente.salida.writeUTF(mensaje);
                    return;
                }
 
                for (unCliente cliente : ServidorMulti.clientes.values()) {
                    cliente.salida.writeUTF(mensaje);
                }
 
            } catch (IOException ex) {
            }
        }
    }
}
