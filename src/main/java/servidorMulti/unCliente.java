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
    private int mensajesEnviados = 0;
    private boolean autenticado = false;
    private String nombreUsuario = null;
 
    unCliente(Socket s, String id) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        this.idCliente = id;
    }
 
    @Override
    public void run() {
        try {
            // Mensaje de bienvenida
            salida.writeUTF("Conectado al servidor. ID: #" + idCliente);
            salida.flush();
            
            while (true) {
                String mensaje = entrada.readUTF();
 
                // Comando de registro
                if (mensaje.startsWith("/registro ")) {
                    procesarRegistro(mensaje);
                    continue;
                }
                
                // Comando de login
                if (mensaje.startsWith("/login ")) {
                    procesarLogin(mensaje);
                    continue;
                }
                
                // Salir
                if ("salir".equalsIgnoreCase(mensaje)) {
                    break;
                }
                
                // Verificar límite de mensajes
                if (!autenticado) {
                    if (mensajesEnviados >= 3) {
                        salida.writeUTF("limite de mensajes alcanzado. Debes registrarte o iniciar sesion.");
                        salida.flush();
                        continue;
                    }
                    mensajesEnviados++;
                }
                
                // Mensaje privado
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    String aQuien = partes[0].substring(1);
                    String contenido = partes.length > 1 ? partes[1] : "";
                    
                    unCliente cliente = ServidorMulti.clientes.get(aQuien);
                    if (cliente != null) {
                        String remitente = autenticado ? nombreUsuario : "Cliente #" + idCliente;
                        cliente.salida.writeUTF("[Privado de " + remitente + "]: " + contenido);
                        cliente.salida.flush();
                    } else {
                        salida.writeUTF("Cliente no encontrado: " + aQuien);
                        salida.flush();
                    }
                } else {
                    // Broadcast a todos excepto al remitente
                    String remitente = autenticado ? nombreUsuario : "Cliente #" + idCliente;
                    for (unCliente cliente : ServidorMulti.clientes.values()) {
                        if (!cliente.idCliente.equals(this.idCliente)) {
                            cliente.salida.writeUTF("[" + remitente + "]: " + mensaje);
                            cliente.salida.flush();
                        }
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Cliente #" + idCliente + " desconectado");
        } finally {
            ServidorMulti.clientes.remove(idCliente);
            try {
                entrada.close();
                salida.close();
            } catch (IOException ignored) {}
        }
    }
    
    private void procesarRegistro(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ");
        if (partes.length != 3) {
            salida.writeUTF("Formato incorrecto. Usa: /registro <usuario> <password>");
            salida.flush();
            return;
        }
        
        String usuario = partes[1];
        String password = partes[2];
        
        synchronized (ServidorMulti.usuarios) {
            if (ServidorMulti.usuarios.containsKey(usuario)) {
                salida.writeUTF("El usuario '" + usuario + "' ya existe.");
                salida.flush();
                return;
            }
            
            // Guardar en memoria
            ServidorMulti.usuarios.put(usuario, password);
            
            // Guardar en archivo (PERSISTENCIA)
            ServidorMulti.guardarUsuario(usuario, password);
            
            autenticado = true;
            nombreUsuario = usuario;
            mensajesEnviados = 0;
            
            salida.writeUTF("Registro exitoso. Bienvenido, " + usuario + "!");
            salida.writeUTF("Ahora puedes enviar mensajes ilimitados.");
            salida.flush();
            
            System.out.println("Usuario registrado: " + usuario);
        }
    }
    
    private void procesarLogin(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ");
        if (partes.length != 3) {
            salida.writeUTF("Formato incorrecto. Usa: /login <usuario> <password>");
            salida.flush();
            return;
        }
        
        String usuario = partes[1];
        String password = partes[2];
        
        synchronized (ServidorMulti.usuarios) {
            if (!ServidorMulti.usuarios.containsKey(usuario)) {
                salida.writeUTF("El usuario '" + usuario + "' no existe.");
                salida.flush();
                return;
            }
            
            if (!ServidorMulti.usuarios.get(usuario).equals(password)) {
                salida.writeUTF("Contraseña incorrecta.");
                salida.flush();
                return;
            }
            
            autenticado = true;
            nombreUsuario = usuario;
            mensajesEnviados = 0;
            
            salida.writeUTF("Inicio de sesion exitoso. Bienvenido de nuevo, " + usuario + "!");
            salida.writeUTF("Ahora puedes enviar mensajes ilimitados.");
            salida.flush();
            
            System.out.println("Usuario autenticado: " + usuario);
        }
    }
}