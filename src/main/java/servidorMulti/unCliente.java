package servidorMulti;

import java.io.*;
import java.net.Socket;

public class unCliente implements Runnable {
    final DataOutputStream salida;
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

                // Limite de mensajes
                if (!autenticado) {
                    if (mensajesEnviados >= 3) {
                        salida.writeUTF("⚠️ Límite de mensajes alcanzado. Regístrate o inicia sesión.");
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
                    // Broadcast
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

        if (DatabaseManager.usuarioExiste(usuario)) {
            salida.writeUTF("El usuario '" + usuario + "' ya existe.");
            salida.flush();
            return;
        }

        boolean exito = DatabaseManager.registrarUsuario(usuario, password);
        if (exito) {
            autenticado = true;
            nombreUsuario = usuario;
            mensajesEnviados = 0;

            salida.writeUTF("Registro exitoso. Bienvenido, " + usuario + "!");
            salida.writeUTF("Ahora puedes enviar mensajes ilimitados.");
            salida.flush();

            System.out.println("🆕 Usuario registrado en DB: " + usuario);
        } else {
            salida.writeUTF("❌ Error al registrar usuario.");
            salida.flush();
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

        if (!DatabaseManager.usuarioExiste(usuario)) {
            salida.writeUTF("El usuario '" + usuario + "' no existe.");
            salida.flush();
            return;
        }

        boolean valido = DatabaseManager.verificarLogin(usuario, password);
        if (!valido) {
            salida.writeUTF("Contraseña incorrecta.");
            salida.flush();
            return;
        }

        autenticado = true;
        nombreUsuario = usuario;
        mensajesEnviados = 0;

        salida.writeUTF("Inicio de sesión exitoso. Bienvenido, " + usuario + "!");
        salida.writeUTF("Ahora puedes enviar mensajes ilimitados.");
        salida.flush();

        System.out.println("✅ Usuario autenticado: " + usuario);
    }
}
