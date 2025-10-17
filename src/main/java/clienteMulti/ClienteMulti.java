package clienteMulti;

import java.io.IOException;
import java.net.Socket;

public class ClienteMulti {
    public static void main(String[] args) throws IOException {
        
        Socket s = null;
        try {
            s = new Socket("localhost", 8080);
 
            ParaMandar paraMandar = new ParaMandar(s);
            ParaRecibir paraRecibir = new ParaRecibir(s);
            
            paraMandar.setParaRecibir(paraRecibir);
            
            Thread hiloParaMandar = new Thread(paraMandar, "sender");
            Thread hiloParaRecibir = new Thread(paraRecibir, "receiver");
 
            hiloParaMandar.start();
            hiloParaRecibir.start();
 
            hiloParaMandar.join();
 
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            if (s != null && !s.isClosed()) {
                try { s.close(); } catch (IOException ignore) {}
            }
        }
    }
}