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
            salida.writeUTF("╔════════════════════════════════════════╗");
            salida.writeUTF("║     BIENVENIDO AL CHAT                 ║");
            salida.writeUTF("╚════════════════════════════════════════╝");
            salida.writeUTF("Conectado al servidor. ID: #" + idCliente);
            salida.writeUTF("Comandos: /ayuda para ver todos");
            salida.flush();
            
            while (true) {
                String mensaje = entrada.readUTF();
 
                if (mensaje.startsWith("/registro ")) {
                    procesarRegistro(mensaje);
                    continue;
                }
                
                if (mensaje.startsWith("/login ")) {
                    procesarLogin(mensaje);
                    continue;
                }
                
                if (mensaje.startsWith("/bloquear ")) {
                    procesarBloquear(mensaje);
                    continue;
                }
                
                if (mensaje.startsWith("/desbloquear ")) {
                    procesarDesbloquear(mensaje);
                    continue;
                }
                
                if (mensaje.equals("/bloqueados")) {
                    procesarListaBloqueados();
                    continue;
                }
                
                if (mensaje.equals("/ayuda")) {
                    mostrarAyuda();
                    continue;
                }
                
                if ("salir".equalsIgnoreCase(mensaje)) {
                    salida.writeUTF("👋 ¡Hasta pronto!");
                    salida.flush();
                    break;
                }
                
                if (!autenticado) {
                    if (mensajesEnviados >= 3) {
                        salida.writeUTF("⚠️ Limite alcanzado. Usa: /registro o /login");
                        salida.flush();
                        continue;
                    }
                    mensajesEnviados++;
                }
                
                if (mensaje.startsWith("@")) {
                    enviarMensajePrivado(mensaje);
                } else {
                    enviarBroadcast(mensaje);
                }
            }
        } catch (IOException ex) {
            System.out.println("[DESCONEXIÓN] Cliente #" + idCliente);
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
            salida.writeUTF("❌ Usa: /registro <usuario> <password>");
            salida.flush();
            return;
        }
        
        String usuario = partes[1];
        String password = partes[2];
        
        if (usuario.length() < 3) {
            salida.writeUTF("❌ Usuario mínimo 3 caracteres");
            salida.flush();
            return;
        }
        
        if (password.length() < 4) {
            salida.writeUTF("❌ Contraseña mínimo 4 caracteres");
            salida.flush();
            return;
        }
        
        if (DatabaseManager.registrarUsuario(usuario, password)) {
            autenticado = true;
            nombreUsuario = usuario;
            mensajesEnviados = 0;
            
            salida.writeUTF("✅ Registro exitoso. Bienvenido, " + usuario + "!");
            salida.writeUTF("Ahora puedes enviar mensajes ilimitados.");
            salida.flush();
        } else {
            salida.writeUTF("❌ El usuario '" + usuario + "' ya existe.");
            salida.flush();
        }
    }
    
    private void procesarLogin(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ");
        if (partes.length != 3) {
            salida.writeUTF("❌ Usa: /login <usuario> <password>");
            salida.flush();
            return;
        }
        
        String usuario = partes[1];
        String password = partes[2];
        
        if (!DatabaseManager.usuarioExiste(usuario)) {
            salida.writeUTF("❌ El usuario '" + usuario + "' no existe.");
            salida.flush();
            return;
        }
        
        if (!DatabaseManager.verificarLogin(usuario, password)) {
            salida.writeUTF("❌ Contraseña incorrecta.");
            salida.flush();
            return;
        }
        
        autenticado = true;
        nombreUsuario = usuario;
        mensajesEnviados = 0;
        
        salida.writeUTF("✅ Inicio de sesion exitoso. Bienvenido, " + usuario + "!");
        salida.writeUTF("Ahora puedes enviar mensajes ilimitados.");
        salida.flush();
    }
    
    private void procesarBloquear(String mensaje) throws IOException {
        if (!autenticado) {
            salida.writeUTF("❌ Debes estar autenticado");
            salida.flush();
            return;
        }
        
        String[] partes = mensaje.split(" ");
        if (partes.length != 2) {
            salida.writeUTF("❌ Usa: /bloquear <usuario>");
            salida.flush();
            return;
        }
        
        String usuarioABloquear = partes[1];
        int resultado = DatabaseManager.bloquearUsuario(nombreUsuario, usuarioABloquear);
        
        switch (resultado) {
            case 1:
                salida.writeUTF("🚫 Has bloqueado a '" + usuarioABloquear + "'");
                break;
            case -1:
                salida.writeUTF("❌ No puedes bloquearte a ti mismo");
                break;
            case -2:
                salida.writeUTF("❌ El usuario '" + usuarioABloquear + "' no existe");
                break;
            case -3:
                salida.writeUTF("⚠️ Ya está bloqueado");
                break;
            default:
                salida.writeUTF("❌ Error al bloquear");
        }
        salida.flush();
    }
    
    private void procesarDesbloquear(String mensaje) throws IOException {
        if (!autenticado) {
            salida.writeUTF("❌ Debes estar autenticado");
            salida.flush();
            return;
        }
        
        String[] partes = mensaje.split(" ");
        if (partes.length != 2) {
            salida.writeUTF("❌ Usa: /desbloquear <usuario>");
            salida.flush();
            return;
        }
        
        String usuarioADesbloquear = partes[1];
        int resultado = DatabaseManager.desbloquearUsuario(nombreUsuario, usuarioADesbloquear);
        
        switch (resultado) {
            case 1:
                salida.writeUTF("✅ Has desbloqueado a '" + usuarioADesbloquear + "'");
                break;
            case -1:
                salida.writeUTF("⚠️ No está bloqueado");
                break;
            default:
                salida.writeUTF("❌ Error al desbloquear");
        }
        salida.flush();
    }
    
    private void procesarListaBloqueados() throws IOException {
        if (!autenticado) {
            salida.writeUTF("❌ Debes estar autenticado");
            salida.flush();
            return;
        }
        
        String lista = DatabaseManager.obtenerListaBloqueados(nombreUsuario);
        int cantidad = DatabaseManager.contarBloqueados(nombreUsuario);
        salida.writeUTF("📋 Bloqueados (" + cantidad + "): " + lista);
        salida.flush();
    }
    
    private void mostrarAyuda() throws IOException {
        salida.writeUTF("\n══════════ COMANDOS DISPONIBLES ══════════");
        salida.writeUTF("🔐 /registro <usuario> <password>");
        salida.writeUTF("🔐 /login <usuario> <password>");
        salida.writeUTF("💬 mensaje - Enviar a todos");
        salida.writeUTF("💬 @usuario mensaje - Privado");
        salida.writeUTF("🚫 /bloquear <usuario>");
        salida.writeUTF("✅ /desbloquear <usuario>");
        salida.writeUTF("📋 /bloqueados");
        salida.writeUTF("❓ /ayuda - Esta ayuda");
        salida.writeUTF("👋 salir - Cerrar conexión");
        salida.writeUTF("═══════════════════════════════════════════\n");
        salida.flush();
    }
    
    private void enviarMensajePrivado(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ", 2);
        String aQuien = partes[0].substring(1);
        String contenido = partes.length > 1 ? partes[1] : "";
        
        unCliente clienteDestino = null;
        for (unCliente c : ServidorMulti.clientes.values()) {
            if (c.autenticado && c.nombreUsuario != null && c.nombreUsuario.equals(aQuien)) {
                clienteDestino = c;
                break;
            }
        }
        
        if (clienteDestino == null) {
            salida.writeUTF("❌ Usuario '" + aQuien + "' no conectado");
            salida.flush();
            return;
        }
        
        if (autenticado && DatabaseManager.estaBloquedo(clienteDestino.nombreUsuario, nombreUsuario)) {
            salida.writeUTF("❌ No puedes enviar mensajes a '" + aQuien + "'");
            salida.flush();
            return;
        }
        
        if (autenticado && DatabaseManager.estaBloquedo(nombreUsuario, clienteDestino.nombreUsuario)) {
            salida.writeUTF("❌ Has bloqueado a '" + aQuien + "'");
            salida.flush();
            return;
        }
        
        String remitente = autenticado ? nombreUsuario : "Cliente #" + idCliente;
        clienteDestino.salida.writeUTF("📨 [Privado de " + remitente + "]: " + contenido);
        clienteDestino.salida.flush();
        salida.writeUTF("✉️ [Enviado a " + aQuien + "]");
        salida.flush();
    }
    
    private void enviarBroadcast(String mensaje) throws IOException {
        String remitente = autenticado ? nombreUsuario : "Cliente #" + idCliente;
        
        for (unCliente cliente : ServidorMulti.clientes.values()) {
            if (!cliente.idCliente.equals(this.idCliente)) {
                if (autenticado && cliente.autenticado && 
                    DatabaseManager.estaBloquedo(cliente.nombreUsuario, nombreUsuario)) {
                    continue;
                }
                
                if (autenticado && cliente.autenticado && 
                    DatabaseManager.estaBloquedo(nombreUsuario, cliente.nombreUsuario)) {
                    continue;
                }
                
                cliente.salida.writeUTF("💬 [" + remitente + "]: " + mensaje);
                cliente.salida.flush();
            }
        }
    }
}