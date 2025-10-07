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
    final String idCliente;
 
    unCliente(Socket s, String id) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        this.idCliente = id;
    }
 
    @Override
    public void run() {
        while (true) {
            try {
                String mensaje = entrada.readUTF();
 
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    String aQuien = partes[0].substring(1);
                    String contenido = partes.length > 1 ? partes[1] : "";
                    
                    unCliente cliente = ServidorMulti.clientes.get(aQuien);
                    if (cliente != null) {
                        // Enviar mensaje privado SOLO al destinatario (no al remitente)
                        cliente.salida.writeUTF("[Privado de Cliente #" + idCliente + "]: " + contenido);
                        cliente.salida.flush();
                    }
                } else {
                    // Broadcast a todos EXCEPTO al remitente
                    for (unCliente cliente : ServidorMulti.clientes.values()) {
                        // Verificar que no sea el mismo cliente que envió el mensaje
                        if (!cliente.idCliente.equals(this.idCliente)) {
                            cliente.salida.writeUTF("[Cliente #" + idCliente + "]: " + mensaje);
                            cliente.salida.flush();
                        }
                    }
                }
 
            } catch (IOException ex) {
                System.out.println("Cliente #" + idCliente + " desconectado");
                ServidorMulti.clientes.remove(idCliente);
                break;
            }
        }
    }
}